import groovy.json.JsonSlurper
import java.util.zip.ZipFile

plugins {
    id("dev.isxander.modstitch.base")
    id("com.gradleup.shadow")
    alias(libs.plugins.buildconfig)
}

val mcVersion = stonecutter.current.version
val loader = "fabric"
val plasmoVoiceVersion =
    libs.versions.plasmovoice
        .asProvider()
        .get()

fun propertyVersion(name: String) = property("versions.$name") as String

val shaded: Configuration by configurations.creating

sourceSets {
    main {
        compileClasspath += shaded
        runtimeClasspath += shaded
    }
}

modstitch {
    minecraftVersion = mcVersion

    loom {
        fabricLoaderVersion = propertyVersion("fabric_loader")

        configureLoom {
            mods {
                maybeCreate("pv_addon_groups").apply {
                    sourceSet(sourceSets.main.get())
                    configuration(shaded)
                }
            }
        }
    }

    metadata {
        modId = "${rootProject.name}-client"
        modName = rootProject.name
        modVersion = "${rootProject.version}+$mcVersion+$loader"
        modGroup = rootProject.group.toString()
        modDescription = "Client-side companion for the pv-addon-groups"
        modLicense = "MIT"

        replacementProperties.put("plasmovoice_version", plasmoVoiceVersion)
        replacementProperties.put("minecraft_range", property("minecraft_range") as String)
    }

    mixin {
        addMixinsToModManifest = true

        configs.register("pv_addon_groups") {
            side = CLIENT
        }
    }
}

java {
    toolchain.languageVersion = modstitch.javaVersion.map(JavaLanguageVersion::of)
}

val nestedJarMods = mutableListOf<Any>()

fun DependencyHandler.modLocalRuntimeNested(notation: Any) {
    add("modstitchModLocalRuntime", notation)
    nestedJarMods += notation
}

fun ZipFile.nestedJarEntries(): List<String> {
    val modJson = getEntry("fabric.mod.json") ?: return emptyList()
    val json = getInputStream(modJson).use { JsonSlurper().parse(it) } as Map<*, *>

    return (json["jars"] as? List<*>)
        .orEmpty()
        .mapNotNull { (it as? Map<*, *>)?.get("file") as? String }
}

// loom doesn't put jar-in-jar dependencies of a mod on the dev runtime classpath
fun unpackNestedJars(): List<File> {
    val jars =
        nestedJarMods.map {
            configurations
                .detachedConfiguration(dependencies.create(it))
                .apply { isTransitive = false }
                .singleFile
        }

    val outputDir =
        layout.buildDirectory
            .dir("nested-jars")
            .get()
            .asFile
    val stamp = outputDir.resolve("stamp.txt")
    val fingerprint = jars.joinToString("\n") { "${it.absolutePath}:${it.length()}:${it.lastModified()}" }

    if (stamp.isFile && stamp.readText() == fingerprint) {
        return outputDir.listFiles { file: File -> file.extension == "jar" }.orEmpty().sorted()
    }

    outputDir.deleteRecursively()
    outputDir.mkdirs()

    val unpacked = mutableListOf<File>()
    val queue = ArrayDeque(jars)

    while (queue.isNotEmpty()) {
        ZipFile(queue.removeFirst()).use { zip ->
            for (path in zip.nestedJarEntries()) {
                val entry = zip.getEntry(path) ?: continue
                val target = outputDir.resolve(path.substringAfterLast('/'))
                if (target.exists()) continue

                zip.getInputStream(entry).use { input -> target.outputStream().use { input.copyTo(it) } }
                unpacked += target
                queue += target
            }
        }
    }

    stamp.writeText(fingerprint)
    return unpacked
}

dependencies {
    val plasmoVoice = "maven.modrinth:plasmo-voice:$loader-$mcVersion-$plasmoVoiceVersion"

    modstitchModCompileOnly(plasmoVoice)
    modLocalRuntimeNested(plasmoVoice)

    listOf(":api", ":common", ":server").forEach {
        shaded(project(it)) {
            isTransitive = false
        }
    }
    shaded(rootProject.libs.mcdsl.brigadier) {
        isTransitive = false
    }
    shaded(rootProject.libs.msgpack) {
        isTransitive = false
    }

    compileOnly(rootProject.libs.kotlinx.coroutines)

    modstitchModLocalRuntime("net.fabricmc.fabric-api:fabric-api:${propertyVersion("fabric_api")}")

    modstitchModLocalRuntime(kotlin("stdlib"))
    modstitchModLocalRuntime(rootProject.libs.kotlinx.json)
    modstitchModLocalRuntime(rootProject.libs.kotlinx.coroutines)

    modstitchModLocalRuntime(files(provider { unpackNestedJars() }))
}

buildConfig {
    packageName = "su.plo.voice.groups.client"

    useJavaOutput()

    buildConfigField("VERSION", modstitch.metadata.modVersion)
    buildConfigField("ADDON_ID", modstitch.metadata.modId)
}

tasks {
    shadowJar {
        configurations = listOf(shaded)

        relocate("kotlin", "su.plo.voice.libs.kotlin")
        relocate("kotlinx.coroutines", "su.plo.voice.libs.kotlinx.coroutines")
        relocate("kotlinx.serialization", "su.plo.voice.libs.kotlinx.serialization")

        relocate("dev.apehum.mcdsl", "su.plo.voice.groups.libs.mcdsl")
        relocate("com.ensarsarajcic.kotlinx.serialization.msgpack", "su.plo.voice.groups.libs.msgpack")
    }

    if (modstitch.platform.isLoomRemap) {
        named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
            dependsOn(shadowJar)
            inputFile.set(shadowJar.flatMap { it.archiveFile })
        }
    }
}

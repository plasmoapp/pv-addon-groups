import groovy.json.JsonSlurper
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import java.util.zip.ZipFile

plugins {
    id("dev.isxander.modstitch.base")
    id("com.gradleup.shadow")
    alias(libs.plugins.buildconfig)
}

val mcVersion = stonecutter.current.version
val loader = stonecutter.current.project.substringAfterLast('-')
val target = stonecutter.current.project.substringBeforeLast('-')
val isFabric = loader == "fabric"
val plasmoVoiceVersion =
    libs.versions.plasmovoice
        .asProvider()
        .get()

fun propertyVersion(name: String) = property("versions.$name") as String

val shaded: Configuration by configurations.creating
val shadedProjects = listOf(":api", ":common", ":server")

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

    moddevgradle {
        neoForgeVersion = propertyVersion("neoforge")

        defaultRuns(server = false)
    }

    metadata {
        // neoforge doesn't allow dashes in mod ids
        modId = if (isFabric) rootProject.name else rootProject.name.replace('-', '_')
        modName = rootProject.name
        modVersion = "${rootProject.version}+$target+$loader"
        modGroup = rootProject.group.toString()
        modDescription = "Plasmo Voice groups addon with client GUI"
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

base.archivesName = rootProject.name

java {
    toolchain.languageVersion = modstitch.javaVersion.map(JavaLanguageVersion::of)
}

stonecutter {
    replacements.string(current.parsed >= "1.21.11") {
        replace("ResourceLocation", "Identifier")
    }
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
    val plasmoVoice = "maven.modrinth:plasmo-voice:$loader-$target-$plasmoVoiceVersion"

    modstitchModCompileOnly(plasmoVoice)

    shadedProjects.forEach {
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

    if (isFabric) {
        modLocalRuntimeNested(plasmoVoice)
        modstitchModLocalRuntime("net.fabricmc.fabric-api:fabric-api:${propertyVersion("fabric_api")}")

        modstitchModLocalRuntime(kotlin("stdlib"))
        modstitchModLocalRuntime(rootProject.libs.kotlinx.json)
        modstitchModLocalRuntime(rootProject.libs.kotlinx.coroutines)

        modstitchModLocalRuntime(files(provider { unpackNestedJars() }))
    } else {
        modstitchModLocalRuntime(plasmoVoice)

        modstitchLocalRuntime(kotlin("stdlib"))
        modstitchLocalRuntime(rootProject.libs.kotlinx.json)
        modstitchLocalRuntime(rootProject.libs.kotlinx.coroutines)
        modstitchLocalRuntime(rootProject.libs.mcdsl.brigadier) {
            isTransitive = false
        }
        modstitchLocalRuntime(rootProject.libs.msgpack) {
            isTransitive = false
        }
    }
}

if (!isFabric) {
    shadedProjects.forEach { evaluationDependsOn(it) }

    configure<NeoForgeExtension> {
        mods.named("mod") {
            shadedProjects.forEach { sourceSet(project(it).sourceSets.main.get()) }
        }
    }
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
    } else {
        jar {
            archiveClassifier = "dev"
        }

        shadowJar {
            archiveClassifier = ""
        }
    }
}

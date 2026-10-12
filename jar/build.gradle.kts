import xyz.jpenilla.runpaper.task.RunServer

plugins {
    alias(libs.plugins.runpaper)
    id("su.plo.voice.plugin.entrypoints")
}

dependencies {
    compileOnly(project(":proxy"))
    compileOnly(project(":server"))

    compileOnly(libs.plasmovoice.server)
    compileOnly(libs.plasmovoice.proxy)
    compileOnly(libs.mcdsl.brigadier)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.msgpack) {
        isTransitive = false
    }

    shadow(libs.mcdsl.brigadier) {
        isTransitive = false
    }
    shadow(libs.msgpack) {
        isTransitive = false
    }
}

entrypoints {
    minPlasmoVoiceVersion = "2.2.0"

    paperPlugin = true
    paperOpenClassloader = true
}

val platforms =
    setOf(
        project(":api"),
        project(":common"),
        project(":proxy"),
        project(":server"),
    )

platforms.forEach { evaluationDependsOn(":${it.name}") }

sourceSets {
    main {
        java {
            srcDir(
                platforms
                    .map {
                        it.sourceSets.main
                            .get()
                            .allJava.sourceDirectories
                    },
            )
        }

        resources {
            srcDir(
                platforms
                    .map {
                        it.sourceSets.main
                            .get()
                            .resources.sourceDirectories
                    },
            )
        }
    }
}

tasks {
    processResources {
        dependsOn(":common:crowdinDownload")
    }

    jar {
        archiveClassifier.set("dev")
    }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())

        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")
        archiveAppendix.set("")

        relocate("dev.apehum.mcdsl", "su.plo.voice.groups.libs.mcdsl")
        relocate("com.ensarsarajcic.kotlinx.serialization.msgpack", "su.plo.voice.groups.libs.msgpack")
    }

    build {
        dependsOn(shadowJar)

        doLast {
            shadowJar
                .get()
                .archiveFile
                .get()
                .asFile
                .copyTo(rootProject.buildDir.resolve("libs/${shadowJar.get().archiveFile.get().asFile.name}"), true)
        }
    }

    withType<RunServer>().configureEach {
        minecraftVersion("26.3")

        javaLauncher =
            project.javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(25)
            }

        downloadPlugins {
            modrinth("plasmo-voice", "paper-${libs.versions.plasmovoice.asProvider().get()}")
        }
    }
}

runPaper.folia.registerTask {
    minecraftVersion("26.2")

    runDirectory = project.layout.projectDirectory.dir("run-folia")
}

import java.net.URI

plugins {
    alias(libs.plugins.crowdin)
    id("su.plo.voice.plugin.entrypoints")
}

dependencies {
    compileOnly(project(":proxy"))
    compileOnly(project(":server"))

    compileOnly(libs.plasmovoice.server)
    compileOnly(libs.plasmovoice.proxy)
    compileOnly(libs.mcdsl.brigadier)

    shadow(libs.mcdsl.brigadier) {
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

crowdin {
    url = URI.create("https://github.com/plasmoapp/plasmo-voice-crowdin/archive/refs/heads/addons.zip").toURL()
    sourceFileName = "server/groups.toml"
    resourceDir = "groups/languages"
    createList = true
}

tasks {
    jar {
        archiveClassifier.set("dev")
    }

    shadowJar {
        configurations = listOf(project.configurations.shadow.get())

        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")
        archiveAppendix.set("")

        relocate("dev.apehum.mcdsl", "su.plo.voice.groups.libs.mcdsl")
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
}

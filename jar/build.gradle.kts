val plasmoVoiceVersion: String by rootProject

plugins {
    id("su.plo.crowdin.plugin") version("1.0.0")
    id("su.plo.voice.plugin") version("1.0.0")
}

dependencies {
    compileOnly("su.plo.voice.api:server:$plasmoVoiceVersion")
    compileOnly("su.plo.voice.api:proxy:$plasmoVoiceVersion")
}

val platforms = setOf(
    project(":common"),
    project(":proxy"),
    project(":server")
)

platforms.forEach { evaluationDependsOn(":${it.name}") }

sourceSets {
    main {
        java {
            srcDir(platforms.map { it.sourceSets.main.get().allJava.srcDirs }.flatten())
        }

        resources {
            srcDir(platforms.map { it.sourceSets.main.get().resources.srcDirs }.flatten())
        }
    }
}

plasmoCrowdin {
    projectId = "plasmo-voice-addons"
    sourceFileName = "server/groups.toml"
    resourceDir = "groups/languages"
    createList = true
}

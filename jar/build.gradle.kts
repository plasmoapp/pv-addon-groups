val plasmoVoiceVersion: String by rootProject

dependencies {
    compileOnly("su.plo.voice.api:server:$plasmoVoiceVersion")
    compileOnly("su.plo.voice.api:proxy:$plasmoVoiceVersion")

    kapt("su.plo.voice.api:server:$plasmoVoiceVersion")
}

val platforms = setOf(
    project(":common"),
    project(":proxy"),
    project(":server")
)

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

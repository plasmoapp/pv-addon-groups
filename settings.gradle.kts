pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.plasmoverse.com/snapshots")
        maven("https://repo.plasmoverse.com/releases")
        maven("https://jitpack.io/")
    }
}

rootProject.name = "pv-addon-groups"

include("common", "server", "proxy", "jar")

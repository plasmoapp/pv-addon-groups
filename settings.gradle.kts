pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.plasmoverse.com/releases")
        maven("https://jitpack.io/")
    }
}

rootProject.name = "pv-addon-groups"

include("api", "common", "server", "proxy", "jar")

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.plasmoverse.com/releases")
        maven("https://jitpack.io/")

        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
    }
}

rootProject.name = "pv-addon-groups"

plugins {
    id("dev.kikugie.stonecutter") version "0.9+"
}

include("api", "common", "server", "proxy", "jar")

stonecutter {
    create("client") {
        version("1.21.1-fabric", "1.21.1")

        vcsVersion = "1.21.1-fabric"
    }
}

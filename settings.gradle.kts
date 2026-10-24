pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://repo.plasmoverse.com/releases")
        maven("https://jitpack.io/")

        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
    }
}

rootProject.name = "pv-addon-groups"

plugins {
    id("dev.kikugie.stonecutter") version "0.10-beta.1"
}

include("api", "common", "server", "proxy", "jar")

stonecutter {
    create("client") {
        fun target(
            name: String,
            minecraftVersion: String,
            vararg loaders: String,
        ) = loaders.forEach { version("$name-$it", minecraftVersion) }

        target("1.21.1", "1.21.1", "fabric", "neoforge")
        target("1.21.4", "1.21.4", "neoforge")
        target("1.21.6", "1.21.6", "fabric", "neoforge")
        target("1.21.11", "1.21.11", "neoforge")
        target("26.1", "26.1.2", "fabric")
        target("26.2", "26.2", "fabric", "neoforge")
        target("26.3", "26.3", "fabric", "neoforge")

        vcsVersion = "1.21.1-fabric"
    }
}

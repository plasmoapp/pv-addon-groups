pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        maven("https://repo.plo.su")
        maven("https://jitpack.io/")
        maven("https://maven.minecraftforge.net")
    }
}

rootProject.name = "pv-addon-groups"

include("common", "server", "proxy", "jar")

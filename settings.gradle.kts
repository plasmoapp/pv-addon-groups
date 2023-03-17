pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        maven("https://repo.plo.su")
    }
}

rootProject.name = "pv-addon-groups"

include("common", "server", "proxy", "jar")

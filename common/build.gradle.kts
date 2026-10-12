import java.net.URI

plugins {
    alias(libs.plugins.buildconfig)
    alias(libs.plugins.crowdin)
}

dependencies {
    compileOnly(project(":api"))

    compileOnly(libs.plasmovoice.server.common)
    compileOnly(libs.mcdsl.brigadier)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.msgpack) {
        isTransitive = false
    }
}

buildConfig {
    packageName("su.plo.voice.groups")
    className("BuildConstants")
    useJavaOutput()

    buildConfigField("String", "VERSION", "\"${project.version}\"")
    buildConfigField("String", "ADDON_ID", "\"${rootProject.name}\"")
}

crowdin {
    url = URI.create("https://github.com/plasmoapp/plasmo-voice-crowdin/archive/refs/heads/addons.zip").toURL()
    sourceFileName = "server/groups.toml"
    resourceDir = "groups/languages"
    createList = true
}

tasks {
    processResources {
        dependsOn(crowdinDownload)
    }
}

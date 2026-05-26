plugins {
    alias(libs.plugins.buildconfig)
}

dependencies {
    compileOnly(project(":api"))

    compileOnly(libs.plasmovoice.server.common)
}

buildConfig {
    packageName("su.plo.voice.groups")
    className("BuildConstants")
    useJavaOutput()

    buildConfigField("String", "VERSION", "\"${project.version}\"")
}

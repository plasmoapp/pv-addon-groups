plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("kapt") version "1.6.10"
}

group = "su.plo"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()

    maven {
        url = uri("https://repo.plo.su")
    }
}

dependencies {
    compileOnly("com.google.guava:guava:31.1-jre")
    compileOnly("org.jetbrains:annotations:23.0.0")

    compileOnly("su.plo.voice.api:server:2.0.0+ALPHA")
    compileOnly("su.plo.config:config:1.0.0")

    kapt("su.plo.voice.api:server:2.0.0+ALPHA")
    kapt("com.google.guava:guava:31.1-jre")
    kapt("com.google.code.gson:gson:2.9.0")
}

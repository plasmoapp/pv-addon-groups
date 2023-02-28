plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("kapt") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "su.plo"
version = "1.0.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://repo.plo.su")
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "kotlin-kapt")
    apply(plugin = "kotlinx-serialization")

    dependencies {
        compileOnly("su.plo.config:config:1.0.0")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    }
}

tasks {
    jar {
        dependsOn(project(":jar").tasks.build)

        from(project(":jar").sourceSets.main.get().output)
    }
}

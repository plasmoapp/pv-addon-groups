plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "su.plo"
version = "1.0.3"

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://repo.plo.su")
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "kotlinx-serialization")

    dependencies {
        compileOnly("su.plo.config:config:1.0.0")
        compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    }

    tasks {
        compileKotlin {
            kotlinOptions {
                val key = "-Xjvm-default="
                freeCompilerArgs = freeCompilerArgs.filterNot { it.startsWith(key) } + listOf(key + "all")
            }
        }
    }
}

tasks {
    jar {
        enabled = false
    }
}

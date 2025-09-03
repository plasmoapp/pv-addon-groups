plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
    alias(libs.plugins.plasmovoice) apply false
    alias(libs.plugins.plasmovoice.java.templates)
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://repo.plasmoverse.com/snapshots")
        maven("https://repo.plasmoverse.com/releases")
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "kotlinx-serialization")

    dependencies {
        compileOnly(rootProject.libs.config)
        compileOnly(rootProject.libs.kotlinx.json)
    }

    tasks {
        compileKotlin {
            kotlinOptions {
                val key = "-Xjvm-default="
                freeCompilerArgs = freeCompilerArgs.filterNot { it.startsWith(key) } + listOf(key + "all")
            }
        }

        java {
            toolchain.languageVersion.set(JavaLanguageVersion.of(8))
        }
    }
}

tasks {
    jar {
        enabled = false
    }
}

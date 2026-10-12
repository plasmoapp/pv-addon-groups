plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
    alias(libs.plugins.plasmovoice) apply false
    alias(libs.plugins.modstitch) apply false
    alias(libs.plugins.ktlint)
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://repo.plasmoverse.com/snapshots")
        maven("https://repo.plasmoverse.com/releases")
    }

    if (this != project(":jar")) {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")

        ktlint {
            version.set("1.8.0")
            android.set(false)
            ignoreFailures.set(false)
            filter {
                exclude { element ->
                    element.file.path.contains("/build/generated/")
                }
            }
            reporters {
                reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
                reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
            }
        }
    }
}

subprojects {
    if (path == ":client") return@subprojects

    apply(plugin = "kotlin")
    apply(plugin = "kotlinx-serialization")

    dependencies {
        compileOnly(kotlin("stdlib"))
        compileOnly(rootProject.libs.config)
        compileOnly(rootProject.libs.kotlinx.json)
    }

    tasks {
        compileKotlin {
            compilerOptions {
                val key = "-Xjvm-default="
                freeCompilerArgs = freeCompilerArgs.get().filterNot { it.startsWith(key) } + listOf(key + "all")
            }
        }

        java {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}

tasks {
    jar {
        enabled = false
    }
}

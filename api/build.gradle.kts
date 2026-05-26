@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

kotlin {
    abiValidation {
        enabled.set(true)
    }
}

dependencies {
    compileOnly(libs.plasmovoice.server.common)
}

tasks.named("check") {
    dependsOn(tasks.named("checkLegacyAbi"))
}

@file:OptIn(ExperimentalAbiValidation::class)

import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.publish)
}

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

mavenPublishing {
    coordinates(
        groupId = "su.plo.voice.addon",
        artifactId = "groups-api",
    )

    publishToMavenCentral(automaticRelease = true, validateDeployment = false)

    pom {
        name.set("pv-addon-groups")
        description.set("Plasmo Voice groups addon API.")
        inceptionYear.set("2023")
        url.set("https://github.com/plasmoapp/pv-addon-groups")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("https://opensource.org/license/mit")
            }
        }

        developers {
            developer {
                id.set("plasmo")
                name.set("Plasmo")
                url.set("https://github.com/plasmoapp")
            }
        }

        scm {
            url.set("https://github.com/plasmoapp/pv-addon-groups/")
            connection.set("scm:git:git://github.com/plasmoapp/pv-addon-groups.git")
            developerConnection.set("scm:git:ssh://git@github.com:plasmoapp/pv-addon-groups.git")
        }
    }

    val hasSigningKey =
        project.hasProperty("signing.keyId") ||
            project.hasProperty("signingInMemoryKey") ||
            System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey") != null

    if (hasSigningKey) {
        signAllPublications()
    } else {
        logger.warn("Signing credentials not found. Publications will not be signed.")
        logger.warn("Configure signing properties (signing.keyId, signing.password, signing.secretKeyRingFile)")
        logger.warn("or signingInMemoryKey/signingInMemoryKeyPassword to enable signing.")
    }
}

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradle.develocity") version "4.5.0"
    id("com.gradleup.nmcp.settings") version "1.6.1"
}

develocity {
    buildScan {
        termsOfUseUrl.set("https://gradle.com/help/legal-terms-of-use")
        termsOfUseAgree.set("yes")
        uploadInBackground.set(false)

        publishing { onlyIf { System.getenv("GITHUB_ACTIONS") == "true" } }

        if (System.getenv("GITHUB_ACTIONS") == "true") {
            tag("CI")
            value("Git SHA", System.getenv("GITHUB_SHA") ?: "unknown")
            value("Git Ref", System.getenv("GITHUB_REF") ?: "unknown")
            link(
                "GitHub Run",
                "${System.getenv("GITHUB_SERVER_URL")}/${System.getenv("GITHUB_REPOSITORY")}/actions/runs/${System.getenv("GITHUB_RUN_ID")}",
            )
        }
    }
}

nmcpSettings {
    centralPortal {
        username.set(providers.gradleProperty("mavenCentralUsername"))
        password.set(providers.gradleProperty("mavenCentralPassword"))
        publishingType.set("AUTOMATIC")
        validationTimeout.set(java.time.Duration.ofMinutes(30))
        publishingTimeout.set(java.time.Duration.ZERO)
    }
}

// NMCP requires the root project name to differ from the published ":qs-kotlin" module.
rootProject.name = "qs-kotlin-build"

include(
    ":qs-kotlin",
    ":qs-kotlin-android",
    ":qs-kotlin-okhttp",
    ":qs-kotlin-ktor",
    ":qs-kotlin-spring-web",
    ":comparison",
)

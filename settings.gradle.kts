pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradle.develocity") version "4.3.1"
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

rootProject.name = "qs-kotlin"

include(":qs-kotlin", ":qs-kotlin-android", ":comparison")

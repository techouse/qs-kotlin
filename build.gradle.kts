plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("android") version "2.0.21" apply false
    id("com.android.library") version "8.11.1" apply false
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
    id("org.jetbrains.dokka") version "1.9.20" apply false
}

allprojects {
    group = "io.github.techouse"
    version = "1.1.1"
    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    if (name in listOf("qs-kotlin", "qs-kotlin-android")) {
        plugins.withId("org.jetbrains.kotlin.jvm") { apply(plugin = "org.jetbrains.dokka") }
        plugins.withId("org.jetbrains.kotlin.android") { apply(plugin = "org.jetbrains.dokka") }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
            snapshotRepositoryUrl.set(
                uri("https://central.sonatype.com/repository/maven-snapshots/")
            )
            username.set(providers.gradleProperty("mavenCentralUsername"))
            password.set(providers.gradleProperty("mavenCentralPassword"))
        }
    }
}

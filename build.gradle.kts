plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.nexus.publish)
    alias(libs.plugins.dokka) apply false
}

allprojects {
    group = "io.github.techouse"
    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    if (name in listOf("qs-kotlin")) {
        apply(plugin = "org.jetbrains.dokka")
    }
}

tasks.register("docs") { dependsOn("dokkaHtmlMultiModule") }

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

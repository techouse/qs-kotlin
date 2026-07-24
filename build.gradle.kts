plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.dokka) apply false
}

allprojects {
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

tasks.register("docs") { dependsOn("dokkaGenerate") }

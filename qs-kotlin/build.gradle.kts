import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    id("com.ncorti.ktfmt.gradle") version "0.23.0"
    id("jacoco")
    `maven-publish`
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }

jacoco { toolVersion = "0.8.13" }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.apply {
        jvmTarget.set(JvmTarget.JVM_17)
        // keep language/api at 2.0 for maximum consumer compatibility
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy("jacocoJvmReport")
}

tasks.register<JacocoReport>("jacocoJvmReport") {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoJvmReport/html"))
        xml.outputLocation.set(
            layout.buildDirectory.file("reports/jacoco/jacocoJvmReport/jacoco.xml")
        )
    }
    classDirectories.setFrom(files(layout.buildDirectory.dir("classes/kotlin/main")))
    sourceDirectories.setFrom(files("src/main/kotlin"))
    executionData.setFrom(files(layout.buildDirectory.file("jacoco/test.exec")))
}

repositories { mavenCentral() }

dependencies {
    testImplementation(platform("io.kotest:kotest-bom:5.9.1"))
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
}

tasks.test { useJUnitPlatform() }

ktfmt { kotlinLangStyle() }

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("qs-kotlin")
                description.set(
                    "A query string encoding and decoding library for Kotlin/JVM. Ported from qs for JavaScript."
                )
                url.set("https://github.com/techouse/qs-kotlin")
            }
        }
    }
}

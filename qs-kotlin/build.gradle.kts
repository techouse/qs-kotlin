import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    `maven-publish`
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.apply {
        jvmTarget.set(JvmTarget.JVM_17)
        // keep language/api at 2.0 for maximum consumer compatibility
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

repositories { mavenCentral() }

dependencies {
    testImplementation(platform("io.kotest:kotest-bom:5.9.1"))
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
}

tasks.test { useJUnitPlatform() }

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

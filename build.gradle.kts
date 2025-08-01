import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.0"
    `maven-publish`
}

group = "io.github.techouse"

version = "1.0-SNAPSHOT"

java {
    // compile *with* JDK 17 and produce Java-17 bytecode
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.ncorti.ktfmt.gradle") version "0.23.0"
    jacoco
    `maven-publish`
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
    withJavadocJar()
}

jacoco { toolVersion = "0.8.13" }

tasks.withType<KotlinCompile> {
    compilerOptions.apply {
        jvmTarget.set(JvmTarget.JVM_17)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

dependencies {
    testImplementation(platform("io.kotest:kotest-bom:5.9.1"))
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
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

ktfmt { kotlinLangStyle() }

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("qs-kotlin")
                description.set(
                    "A query string encoding and decoding library for Android and Kotlin/JVM. Ported from qs for JavaScript."
                )
                url.set("https://github.com/techouse/qs-kotlin")
                licenses {
                    license {
                        name.set("BSD-3-Clause License")
                        url.set("https://opensource.org/license/bsd-3-clause")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/techouse/qs-kotlin.git")
                    developerConnection.set("scm:git:ssh://git@github.com/techouse/qs-kotlin.git")
                    url.set("https://github.com/techouse/qs-kotlin")
                }
                developers {
                    developer {
                        id.set("techouse")
                        name.set("Klemen Tusar")
                        email.set("techouse@gmail.com")
                        url.set("https://github.com/techouse")
                    }
                }
            }
        }
    }
}

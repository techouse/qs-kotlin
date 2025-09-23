import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.spotless)
    jacoco
    `maven-publish`
    signing
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
    withJavadocJar()
}

jacoco { toolVersion = libs.versions.jacoco.get() }

tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<KotlinCompile> { compilerOptions.apply { jvmTarget.set(JvmTarget.JVM_17) } }

dependencies {
    testImplementation(platform(libs.kotest.bom))
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
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

spotless {
    java {
        googleJavaFormat(libs.versions.googleJavaFormat.get()) // pick a version you like
        target("src/**/*.java")
    }
}

val prepareDokkaReadme by
    tasks.registering {
        val src = rootProject.layout.projectDirectory.file("README.md").asFile
        val outDir = layout.buildDirectory.dir("dokka-includes").get().asFile
        val out = outDir.resolve("Module.md")

        outputs.file(out)
        doLast {
            out.parentFile.mkdirs()
            val readme = src.readText()

            // Remove the first H1 (e.g., "# qs-kotlin") so we don’t have two top-level titles
            val withoutFirstH1 = readme.replaceFirst(Regex("""^\s*# .*\R+"""), "")

            // Prepend Dokka’s required classifier header
            val moduleHeader = "# Module qs-kotlin\n\n"
            out.writeText(moduleHeader + withoutFirstH1)
        }
    }

tasks.dokkaHtml {
    dependsOn(prepareDokkaReadme)
    dokkaSourceSets.configureEach {
        includes.from(layout.buildDirectory.file("dokka-includes/Module.md"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("qs-kotlin")
                description.set(
                    "A query string encoding and decoding library for Android and Kotlin/JVM. Ported from qs for JavaScript."
                )
                url.set("https://techouse.github.io/qs-kotlin/")
                inceptionYear.set("2025")
                licenses {
                    license {
                        name.set("BSD-3-Clause")
                        url.set("https://github.com/techouse/qs-kotlin/blob/main/LICENSE")
                        distribution.set("repo")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/techouse/qs-kotlin.git")
                    developerConnection.set("scm:git:ssh://git@github.com/techouse/qs-kotlin.git")
                    url.set("https://github.com/techouse/qs-kotlin")
                    tag.set("HEAD")
                }
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/techouse/qs-kotlin/issues")
                }
                ciManagement {
                    system.set("GitHub Actions")
                    url.set("https://github.com/techouse/qs-kotlin/actions")
                }
                developers {
                    developer {
                        id.set("techouse")
                        name.set("Klemen Tusar")
                        email.set("techouse@gmail.com")
                        url.set("https://github.com/techouse")
                        roles.set(listOf("Lead", "Maintainer"))
                        timezone.set("Europe/London")
                        properties.put("twitter", "https://x.com/nextk2")
                        properties.put("linkedin", "https://www.linkedin.com/in/techouse/")
                        properties.put("sponsor", "https://github.com/sponsors/techouse")
                        properties.put("paypal", "https://paypal.me/ktusar")
                    }
                }
                properties.put(
                    "changelogUrl",
                    "https://github.com/techouse/qs_codec/blob/master/CHANGELOG.md",
                )
            }
        }
    }
}

signing {
    val hasKey = providers.gradleProperty("signingInMemoryKey").isPresent
    isRequired = hasKey && !project.version.toString().endsWith("SNAPSHOT")
    if (hasKey) {
        useInMemoryPgpKeys(
            providers.gradleProperty("signingInMemoryKey").get(),
            providers.gradleProperty("signingInMemoryKeyPassword").getOrElse(""),
        )
        sign(publishing.publications)
    }
}

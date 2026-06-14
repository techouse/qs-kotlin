import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktfmt)
    `maven-publish`
    signing
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<KotlinCompile>().configureEach { compilerOptions.jvmTarget.set(JvmTarget.JVM_17) }

dependencies {
    api(project(":qs-kotlin"))
    api(libs.okhttp)

    testImplementation(platform(libs.kotest.bom))
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
}

tasks.test { useJUnitPlatform() }

ktfmt { kotlinLangStyle() }

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("qs-kotlin-okhttp")
                description.set(
                    "OkHttp HttpUrl integration for qs-kotlin nested query string encoding."
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

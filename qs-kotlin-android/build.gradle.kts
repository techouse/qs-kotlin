import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("android")
    id("com.android.library")
    id("com.ncorti.ktfmt.gradle") version "0.23.0"
    `maven-publish`
    signing
}

android {
    namespace = "io.github.techouse.qskotlin.android"
    compileSdk = 36
    defaultConfig { minSdk = 25 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        jvmToolchain(17)

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
            apiVersion.set(KotlinVersion.KOTLIN_2_0)
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    api(project(":qs-kotlin"))
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}

ktfmt { kotlinLangStyle() }

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = project.group.toString()
                artifactId = "qs-kotlin-android"
                version = project.version.toString()

                pom {
                    name.set("qs-kotlin-android")
                    description.set(
                        "Android (AAR) wrapper for qs-kotlin — query string encoding/decoding ported from qs (JS)."
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
                        url.set("https://github.com/techouse/qs-kotlin")
                        connection.set("scm:git:https://github.com/techouse/qs-kotlin.git")
                        developerConnection.set(
                            "scm:git:ssh://git@github.com/techouse/qs-kotlin.git"
                        )
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
        val isSnapshot = project.version.toString().endsWith("-SNAPSHOT", ignoreCase = true)

        if (hasKey && !isSnapshot) {
            useInMemoryPgpKeys(
                providers.gradleProperty("signingInMemoryKey").get(),
                providers.gradleProperty("signingInMemoryKeyPassword").getOrElse(""),
            )
            sign(publishing.publications)
        }
    }
}

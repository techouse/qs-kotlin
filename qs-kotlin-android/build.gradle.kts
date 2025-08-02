plugins {
    kotlin("android")
    id("com.android.library")
    `maven-publish`
    signing
}

android {
    namespace = "io.github.techouse.qskotlin.android"
    compileSdk = 35
    defaultConfig { minSdk = 25 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }

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
                    description.set("Android (AAR) wrapper for qs-kotlin — query string encoding/decoding ported from qs (JS).")
                    url.set("https://github.com/techouse/qs-kotlin")
                    licenses {
                        license {
                            name.set("BSD-3-Clause License")
                            url.set("https://opensource.org/license/bsd-3-clause")
                        }
                    }
                    scm {
                        url.set("https://github.com/techouse/qs-kotlin")
                        connection.set("scm:git:https://github.com/techouse/qs-kotlin.git")
                        developerConnection.set("scm:git:ssh://git@github.com/techouse/qs-kotlin.git")
                    }
                    developers {
                        developer {
                            id.set("techouse"); name.set("Klemen Tusar")
                            email.set("techouse@gmail.com"); url.set("https://github.com/techouse")
                        }
                    }
                }
            }
        }
    }

    signing {
        sign(publishing.publications)
    }
}

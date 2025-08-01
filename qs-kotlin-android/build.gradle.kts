plugins {
    kotlin("android")
    id("com.android.library")
    `maven-publish`
}

repositories {
    google()
    mavenCentral()
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

    publishing { singleVariant("release") }
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
                groupId = "io.github.techouse"
                artifactId = "qs-kotlin-android"
                version = project.version.toString()
                pom {
                    name.set("qs-kotlin-android")
                    description.set("Android (AAR) wrapper for qs-kotlin")
                    url.set("https://github.com/techouse/qs-kotlin")
                }
            }
        }
    }
}

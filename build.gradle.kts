plugins {
    kotlin("jvm") version "2.0.20" apply false
    kotlin("android") version "2.0.20" apply false
    id("com.android.library") version "8.7.0" apply false
}

allprojects {
    group = "io.github.techouse"
    version = "1.0.0"
    repositories {
        google()
        mavenCentral()
    }
}

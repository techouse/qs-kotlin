plugins {
    kotlin("jvm") version "2.0.20" apply false
    kotlin("android") version "2.0.20" apply false
    id("com.android.library") version "8.7.0" apply false
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

allprojects {
    group = "io.github.techouse"
    version = "1.0.0"
    repositories {
        google()
        mavenCentral()
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(providers.gradleProperty("mavenCentralUsername"))
            password.set(providers.gradleProperty("mavenCentralPassword"))
        }
    }
}

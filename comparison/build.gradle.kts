plugins {
    kotlin("jvm")
    application
}

repositories { mavenCentral() }

dependencies {
    implementation(project(":qs-kotlin"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
}

application { mainClass.set("io.github.techouse.qskotlin.compare.MainKt") }

tasks
    .matching { t ->
        t.name.startsWith("publish", ignoreCase = true) ||
            t.name.startsWith("sign", ignoreCase = true) ||
            t.name.startsWith("dokka", ignoreCase = true)
    }
    .configureEach { enabled = false }

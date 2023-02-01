plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `maven-publish`
    id("app.cash.sqldelight") version "2.0.0-SNAPSHOT"
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r21"

kotlin {
    jvmToolchain(8)
}

java {
    withSourcesJar()
}

dependencies {
    implementation(project(":models"))
    implementation(project(":kotlinx-vdf"))

    implementation("io.ktor:ktor-serialization:2.2.2")
    implementation("io.ktor:ktor-client-core:2.2.2")
    implementation("io.ktor:ktor-client-websockets:2.2.2")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.2")
    implementation("io.ktor:ktor-client-cio:2.2.2")
    implementation("io.ktor:ktor-client-okhttp:2.2.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.4.1")

    implementation("app.cash.sqldelight:async-extensions:2.0.0-SNAPSHOT")

    implementation("com.squareup.okio:okio:3.2.0")
    api("com.squareup.wire:wire-runtime:4.4.3")

    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set("kSteam - Core")
                description.set("A Steam Network client library with a slice of Kotlin.")
                url.set("https://github.com/itaysonlab/ksteam")
                from(components.findByName("java"))
            }
        }
    }
}

sqldelight {
    databases {
        create("Database") {
            packageName.set("bruhcollective.itaysonlab.ksteam.persist")
        }
    }
}
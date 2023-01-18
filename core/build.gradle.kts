plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r7"

kotlin {
    jvmToolchain(8)
}

java {
    withSourcesJar()
}

dependencies {
    implementation(project(":models"))

    implementation("io.ktor:ktor-serialization:2.2.1")
    implementation("io.ktor:ktor-client-core:2.2.1")
    implementation("io.ktor:ktor-client-websockets:2.2.1")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.1")
    implementation("io.ktor:ktor-client-cio:2.2.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.4.1")

    implementation("com.squareup.okio:okio:3.2.0")
    api("com.squareup.wire:wire-runtime:4.4.3")
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
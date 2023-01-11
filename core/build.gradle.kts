plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation(project(":models"))

    implementation("io.ktor:ktor-serialization:2.2.1")
    implementation("io.ktor:ktor-client-core:2.2.1")
    implementation("io.ktor:ktor-client-websockets:2.2.1")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.1")
    implementation("io.ktor:ktor-client-cio:2.2.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.1")

    implementation("com.squareup.okio:okio:3.2.0")
    api("com.squareup.wire:wire-runtime:4.4.3")
}
plugins {
    kotlin("jvm")
    id("com.squareup.wire")
    `maven-publish`
}

wire {
    kotlin {
        rpcCallStyle = "suspending"
        rpcRole = "server"
        nameSuffix = ""
    }
}

group = "bruhcollective.itaysonlab.ksteam"
version = "b2"

kotlin {
    jvmToolchain(8)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.useK2 = false
}

dependencies {
    api("com.squareup.wire:wire-runtime:4.4.3")
    implementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set("kSteam - Models")
                description.set("Protocol Buffers models for kSteam")
                url.set("https://github.com/itaysonlab/ksteam")
                from(components.findByName("java"))
            }
        }
    }
}
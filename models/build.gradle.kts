plugins {
    kotlin("jvm")
    id("com.squareup.wire")
}

wire {
    kotlin {
        rpcCallStyle = "suspending"
        rpcRole = "server"
        nameSuffix = ""
    }
}

group = "bruhcollective.itaysonlab.ksteam"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(8)
}

dependencies {
    api("com.squareup.wire:wire-runtime:4.4.3")
    implementation(kotlin("test"))
}
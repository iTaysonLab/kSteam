plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.squareup.wire")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r24"

kotlin {

}

java {
    withSourcesJar()
}

wire {
    kotlin {
        rpcCallStyle = "suspending"
        rpcRole = "server"
        nameSuffix = ""
    }
}

dependencies {
    implementation(project(":kotlinx-vdf"))

    implementation(project(":proto-common"))
    protoPath(project(":proto-common"))

    // For @Stable / @Immutable annotations inside "UI" models
    compileOnly("org.jetbrains.compose.runtime:runtime-desktop:1.3.0")

    implementation("io.ktor:ktor-serialization:2.2.2")
    implementation("io.ktor:ktor-client-core:2.2.2")
    implementation("io.ktor:ktor-client-websockets:2.2.2")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.2")
    implementation("io.ktor:ktor-client-cio:2.2.2")
    implementation("io.ktor:ktor-client-okhttp:2.2.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.2")

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    implementation("com.squareup.okio:okio:3.2.0")
    api("com.squareup.wire:wire-runtime:4.5.2")

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
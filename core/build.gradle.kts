plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r23"

kotlin {
    jvmToolchain(8)
}

java {
    withSourcesJar()
}

dependencies {
    implementation(project(":models"))
    implementation(project(":kotlinx-vdf"))

    // For @Stable / @Immutable annotations inside "UI" models
    compileOnly("org.jetbrains.compose.runtime:runtime-desktop:1.3.0")

    implementation("io.ktor:ktor-serialization:2.2.2")
    implementation("io.ktor:ktor-client-core:2.2.2")
    implementation("io.ktor:ktor-client-websockets:2.2.2")
    implementation("io.ktor:ktor-client-content-negotiation:2.2.2")
    implementation("io.ktor:ktor-client-cio:2.2.2")
    implementation("io.ktor:ktor-client-okhttp:2.2.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.4.1")

    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.41.1")
    implementation("com.h2database:h2:2.1.214")

    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")

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
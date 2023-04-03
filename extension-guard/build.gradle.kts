plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.squareup.wire")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r25"

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
    implementation(project(":core"))

    implementation(project(":proto-common"))
    protoPath(project(":proto-common"))

    // For @Stable / @Immutable annotations inside "UI" models
    compileOnly("org.jetbrains.compose.runtime:runtime-desktop:1.3.0")

    implementation("io.ktor:ktor-client-core:2.2.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
    api("com.squareup.wire:wire-runtime:4.5.3")

    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set("kSteam - Guard Extension")
                description.set("Steam Guard extension for kSteam")
                url.set("https://github.com/itaysonlab/ksteam")
                from(components.findByName("java"))
            }
        }
    }
}
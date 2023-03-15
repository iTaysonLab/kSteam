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

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    // For @Stable / @Immutable annotations inside "UI" models
    compileOnly("org.jetbrains.compose.runtime:runtime-desktop:1.3.0")

    api("com.squareup.wire:wire-runtime:4.5.2")

    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set("kSteam - Core Extension")
                description.set("Core extension for kSteam - friends, personas...")
                url.set("https://github.com/itaysonlab/ksteam")
                from(components.findByName("java"))
            }
        }
    }
}
plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.squareup.wire")
    id("build-extensions")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r26"

kotlin {
    jvmToolchain(11)

    jvm()

    configureOrCreateNativePlatforms()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":core"))
                implementation(project(":proto-common"))

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")

                api("com.squareup.wire:wire-runtime:4.5.3")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

wire {
    kotlin {
        rpcCallStyle = "suspending"
        rpcRole = "server"
        nameSuffix = ""
    }

    protoPath {
        srcDir("../proto-common/src/commonMain/proto/")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set("kSteam - Core Extension")
                description.set("Core extension for kSteam - friends, personas...")
                url.set("https://github.com/itaysonlab/ksteam")
            }
        }
    }
}
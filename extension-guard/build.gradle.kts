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
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.4.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

                implementation("com.benasher44:uuid:0.7.0")
                implementation("io.ktor:ktor-client-core:2.2.4")

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
                name.set("kSteam - Guard Extension")
                description.set("Steam Guard extension for kSteam")
                url.set("https://github.com/itaysonlab/ksteam")
                from(components.findByName("java"))
            }
        }
    }
}
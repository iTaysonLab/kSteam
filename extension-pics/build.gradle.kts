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
                api(project(":core"))
                api(project(":proto-common"))
                api(project(":extension-core"))
                api(project(":kotlinx-vdf"))

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-RC")

                api("com.squareup.wire:wire-runtime:4.5.6")
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
                description.set("PICS extension for kSteam")
                url.set("https://github.com/itaysonlab/ksteam")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://www.opensource.org/licenses/mit-license.php")
                    }
                }

                developers {
                    developer {
                        name.set("iTaysonLab")
                        url.set("https://github.com/itaysonlab/")
                    }
                }
            }
        }
    }
}
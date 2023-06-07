plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.squareup.wire")
    id("build-extensions")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r27"

kotlin {
    jvmToolchain(11)

    jvm()

    configureOrCreateNativePlatforms()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":core"))
                api(project(":proto-common"))

                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.5.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

                implementation("com.benasher44:uuid:0.7.0")
                implementation("io.ktor:ktor-client-core:2.3.1")

                api("com.squareup.wire:wire-runtime:4.7.0")
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
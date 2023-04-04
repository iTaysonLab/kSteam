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

    jvm {

    }

    configureOrCreateNativePlatforms(onEachTarget = {
        compilations.getByName("main") {
            cinterops {
                val libdeflate by creating {
                    defFile(project.file("src/nativeInterop/cinterop/libdeflate.def"))
                    compilerOpts("-I/src/nativeInterop/cinterop")

                    includeDirs {
                        allHeaders("src/nativeInterop/cinterop")
                    }
                }
            }
        }
    })

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kotlinx-vdf"))
                api(project(":proto-common"))

                implementation("io.ktor:ktor-serialization:2.2.4")
                implementation("io.ktor:ktor-client-core:2.2.4")
                implementation("io.ktor:ktor-client-websockets:2.2.4")
                implementation("io.ktor:ktor-client-content-negotiation:2.2.4")
                implementation("io.ktor:ktor-client-cio:2.2.4")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.4")

                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.4.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")

                implementation("com.squareup.okio:okio:3.3.0")
                api("com.squareup.wire:wire-runtime:4.5.3")
            }
        }

        val jvmMain by getting {
            dependencies {

            }
        }

        val nativeMain = createSourceSet("nativeMain", parent = commonMain, children = appleTargets, dependencies = {

        })

        createSourceSet("appleMain", parent = nativeMain, children = appleTargets, dependencies = {

        })
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
                name.set("kSteam - Core")
                description.set("A Steam Network client library with a slice of Kotlin.")
                url.set("https://github.com/itaysonlab/ksteam")
                from(components.findByName("java"))
            }
        }
    }
}
plugins {
    id("build-extensions")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("com.squareup.wire")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r29"

kotlin {
    multiplatformSetup(additionalNativeTargetConfig = {
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

    androidDependencies {
        implementation("androidx.compose.runtime:runtime:1.4.3")
    }
}

androidLibrary("bruhcollective.itaysonlab.ksteam.core")

dependencies {
    commonMainApi(project(":kotlinx-vdf"))
    commonMainApi(project(":proto-common"))

    commonMainImplementation("io.ktor:ktor-serialization:2.3.1")
    commonMainImplementation("io.ktor:ktor-client-core:2.3.1")
    commonMainImplementation("io.ktor:ktor-client-websockets:2.3.1")
    commonMainImplementation("io.ktor:ktor-client-content-negotiation:2.3.1")
    commonMainImplementation("io.ktor:ktor-client-cio:2.3.1")
    commonMainImplementation("io.ktor:ktor-serialization-kotlinx-json:2.3.1")

    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.5.1")
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")

    commonMainImplementation("com.squareup.okio:okio:3.3.0")
    commonMainApi("com.squareup.wire:wire-runtime:4.7.0")
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
                description.set("A Kotlin library to access the Steam network")
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
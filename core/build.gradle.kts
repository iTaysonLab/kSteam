plugins {
    id("build-extensions")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("com.squareup.wire")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r36"

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

    commonMainImplementation(libs.ktor.serialization)
    commonMainImplementation(libs.ktor.client)
    commonMainImplementation(libs.ktor.client.websockets)
    commonMainImplementation(libs.ktor.client.contentNegotiation)
    commonMainImplementation(libs.ktor.client.engine.cio)
    commonMainImplementation(libs.ktor.client.jsonSerialization)

    commonMainImplementation(libs.kotlinx.immutables)
    commonMainImplementation(libs.kotlinx.serialization.json.okio)
    commonMainImplementation(libs.kotlinx.datetime)

    commonMainImplementation(libs.kotlinx.coroutines.core)

    commonMainImplementation(libs.okio)

    commonMainApi(libs.wire)
    commonMainApi(libs.wire.grpc)
}

wire {
    kotlin {
        rpcCallStyle = "suspending"
        rpcRole = "client"
    }

    protoPath {
        srcDir("../proto-common/src/commonMain/proto/")
    }
}

/*publishing {
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
}*/
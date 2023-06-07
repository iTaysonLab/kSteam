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
}

dependencies {
    commonMainApi(project(":core"))
    commonMainApi(project(":proto-common"))

    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    commonMainImplementation("io.ktor:ktor-client-core:2.3.1")

    commonMainApi("com.squareup.wire:wire-runtime:4.7.0")

    commonTestImplementation(kotlin("test"))
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
                description.set("Core extension for kSteam")
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
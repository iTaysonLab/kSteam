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
    multiplatformSetup()
}

androidLibrary("bruhcollective.itaysonlab.ksteam.extensions.guard_proto_migration")

dependencies {
    commonMainApi(project(":core"))
    commonMainApi(project(":proto-common"))
    commonMainApi(project(":extension-guard"))

    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.5.1")
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    commonMainImplementation("com.benasher44:uuid:0.7.0")
    commonMainImplementation("io.ktor:ktor-client-core:2.3.1")

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
                name.set("kSteam - Guard Extension (Migration)")
                description.set("kSteam extension to migrate legacy protobuf guard files to mafile jsons")
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
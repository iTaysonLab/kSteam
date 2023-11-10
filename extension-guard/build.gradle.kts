plugins {
    id("build-extensions")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("com.squareup.wire")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r31"

kotlin {
    multiplatformSetup()

    androidDependencies {
        implementation("androidx.compose.runtime:runtime:1.4.3")
    }
}

androidLibrary("bruhcollective.itaysonlab.ksteam.extensions.guard")

dependencies {
    commonMainApi(project(":core"))
    commonMainApi(project(":proto-common"))

    commonMainImplementation(libs.kotlinx.serialization.json)
    commonMainImplementation(libs.kotlinx.serialization.json.okio)
    commonMainImplementation(libs.kotlinx.datetime)

    commonMainImplementation("com.benasher44:uuid:0.7.0")
    commonMainImplementation(libs.ktor.client)

    commonMainApi(libs.wire)
}

wire {
    kotlin {
        rpcCallStyle = "suspending"
        rpcRole = "client"
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
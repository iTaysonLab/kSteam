plugins {
    id("build-extensions")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("org.jetbrains.kotlinx.atomicfu")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r46"

kotlin {
    multiplatformSetup()
}

androidLibrary("bruhcollective.itaysonlab.ksteam.core")

dependencies {
    commonMainApi(project(":kotlinx-vdf"))
    commonMainApi(project(":proto-common"))

    commonMainImplementation(libs.androidx.collection)
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

    commonMainApi(libs.wire.grpc) {
        exclude(group = "com.squareup.okhttp3", module = "okhttp")
    }
}

configurations.all {
    exclude(group = "com.squareup.okhttp3", module = "okhttp")
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
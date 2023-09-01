plugins {
    id("build-extensions")
    kotlin("multiplatform")
    id("com.android.library")
    id("com.squareup.wire")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r30"

kotlin {
    multiplatformSetup()
}

wire {
    protoLibrary = true

    kotlin {

    }
}

androidLibrary("bruhcollective.itaysonlab.ksteam.proto")

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set("kSteam - Protobufs (Common)")
                description.set("Common Protocol Buffers for kSteam")
                url.set("https://github.com/itaysonlab/ksteam")
            }
        }
    }
}
plugins {
    id("build-extensions")
    kotlin("multiplatform")
    id("com.android.library")
    id("com.squareup.wire")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r32"

kotlin {
    multiplatformSetup()
}

wire {
    protoLibrary = true

    kotlin {

    }
}

androidLibrary("bruhcollective.itaysonlab.ksteam.proto")
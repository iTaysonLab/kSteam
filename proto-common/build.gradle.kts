plugins {
    id("build-extensions")
    kotlin("multiplatform")
    id("com.android.library")
    id("com.squareup.wire")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r38"

kotlin {
    multiplatformSetup()
}

wire {
    protoLibrary = true

    kotlin {
        rpcCallStyle = "suspending"
        rpcRole = "client"
        nameSuffix = "Service"
    }
}

dependencies {
    commonMainApi(libs.wire.grpc)
}

androidLibrary("bruhcollective.itaysonlab.ksteam.proto")
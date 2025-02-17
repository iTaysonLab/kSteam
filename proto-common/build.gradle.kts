plugins {
    id("build-extensions")
    kotlin("multiplatform")
    id("com.android.library")
    id("com.squareup.wire")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r46"

kotlin {
    multiplatformSetup()
}

wire {
    kotlin {
        rpcCallStyle = "suspending"
        rpcRole = "client"
        nameSuffix = "Service"
    }
}

dependencies {
    commonMainApi(libs.wire.grpc) {
        exclude(group = "com.squareup.okhttp3", module = "okhttp")
    }
}

configurations.all {
    exclude(group = "com.squareup.okhttp3", module = "okhttp")
}

androidLibrary("bruhcollective.itaysonlab.ksteam.proto")
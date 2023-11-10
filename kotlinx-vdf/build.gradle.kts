plugins {
    id("build-extensions")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    `maven-publish`
}

group = "bruhcollective.itaysonlab"
version = "r31"

kotlin {
    multiplatformSetup()
}

androidLibrary("bruhcollective.itaysonlab.kotlinx_vdf")

dependencies {
    commonMainImplementation(libs.kotlinx.serialization.core)
    commonMainImplementation(libs.okio)
    commonMainImplementation("de.cketti.unicode:kotlin-codepoints-deluxe:0.6.1")
}
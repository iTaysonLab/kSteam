plugins {
    id("build-extensions")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("io.realm.kotlin")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r40"

kotlin {
    multiplatformSetup()

    androidDependencies {
        implementation("androidx.compose.runtime:runtime:1.4.3")
    }
}

androidLibrary("bruhcollective.itaysonlab.ksteam.extensions.core")

dependencies {
    commonMainApi(project(":core"))
    commonMainApi(project(":proto-common"))

    commonMainImplementation(libs.androidx.collection)

    commonMainImplementation(libs.kotlinx.serialization.json)
    commonMainImplementation(libs.kotlinx.serialization.json.okio)
    commonMainImplementation(libs.kotlinx.coroutines.core)
    commonMainImplementation(libs.kotlinx.datetime)

    commonMainImplementation(libs.ktor.client)
    commonMainImplementation(libs.realm)
    commonMainImplementation(libs.cache4k)

    commonTestImplementation(kotlin("test"))
}
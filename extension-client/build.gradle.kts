plugins {
    id("build-extensions")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("androidx.room")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r48"

kotlin {
    multiplatformSetup()
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
    commonMainImplementation(libs.cache4k)

    commonMainImplementation(libs.androidx.room.runtime)
    commonMainImplementation(libs.androidx.sqlite)

    commonTestImplementation(kotlin("test"))
}

dependencies {
    listOf(
        "kspAndroid",
        "kspJvm",
        // "kspIosSimulatorArm64",
        // "kspIosX64",
        // "kspIosArm64",
        "kspCommonMainMetadata",
    ).forEach {
        add(it, libs.androidx.room.compiler)
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.compose") version "1.8.1"
}

group = "bruhcollective.itaysonlab"
version = "1.0-SNAPSHOT"

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(kotlin("reflect"))
    implementation(project(":core"))
    implementation(project(":core-persistence"))
    implementation(project(":kotlinx-vdf"))
    implementation(project(":extension-client"))

    // implementation(project(":extension-guard"))
    // implementation(project(":extension-pics"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    implementation("com.squareup.okio:okio:3.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.slf4j:slf4j-simple:2.0.6")
    implementation("org.bouncycastle:bcprov-jdk15to18:1.78")

    // Compose for UI
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation("com.google.zxing:core:3.5.3")

    implementation(libs.ktor.client.engine.okhttp)
}

compose.desktop {
    application {
        mainClass = "bruhcollective.itaysonlab.ksteam.KsteamAppKt"
    }
}
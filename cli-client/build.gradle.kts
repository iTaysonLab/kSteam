import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

group = "bruhcollective.itaysonlab"
version = "1.0-SNAPSHOT"

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core-persistence"))
    implementation(project(":kotlinx-vdf"))
    implementation(project(":extension-client"))

    // implementation(project(":extension-guard"))
    // implementation(project(":extension-pics"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.1")

    implementation("com.squareup.okio:okio:3.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.slf4j:slf4j-simple:2.0.6")

    implementation("org.bouncycastle:bcprov-jdk15to18:1.72")
}
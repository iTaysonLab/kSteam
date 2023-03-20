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
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
    }
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

kotlin {

}

application {
    mainClass.set("MainKt")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":kotlinx-vdf"))

    implementation(project(":extension-core"))
    implementation(project(":extension-guard"))
    implementation(project(":extension-pics"))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.0-RC")

    implementation("com.squareup.okio:okio:3.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.slf4j:slf4j-simple:2.0.6")

    implementation("org.bouncycastle:bcprov-jdk15to18:1.72")
}
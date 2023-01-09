plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    `maven-publish`
}

group = "bruhcollective.itaysonlab.ksteam"
version = "1.0-SNAPSHOT"

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.options.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        }
    }

    android {
        publishAllLibraryVariants()
    }

    configure(listOf(
        macosArm64(), macosX64(), iosArm64(), iosX64()
    )) {
        binaries {
            sharedLib {
                baseName = "ksteam_core"
            }
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":models"))

                implementation("io.ktor:ktor-serialization:2.2.1")
                implementation("io.ktor:ktor-client-core:2.2.1")
                implementation("io.ktor:ktor-client-websockets:2.2.1")
                implementation("io.ktor:ktor-client-content-negotiation:2.2.1")
                implementation("io.ktor:ktor-client-cio:2.2.1")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.2.1")

                implementation("com.squareup.okio:okio:3.2.0")
                api("com.squareup.wire:wire-runtime:4.4.3")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val darwinMain by creating {
            this.dependsOn(commonMain)
        }

        val macosArm64Main by getting {
            this.dependsOn(darwinMain)
        }

        val macosX64Main by getting {
            this.dependsOn(darwinMain)
        }

        val iosArm64Main by getting {
            this.dependsOn(darwinMain)
        }

        val iosX64Main by getting {
            this.dependsOn(darwinMain)
        }

        val jvmMain by getting
        val jvmTest by getting
    }
}

android {
    buildToolsVersion = "32.0.0"
    compileSdk = 31

    defaultConfig {
        minSdk = 17
        targetSdk = 31
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    testCoverage.jacocoVersion = "0.8.8"
}
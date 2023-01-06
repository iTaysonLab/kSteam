plugins {
    kotlin("multiplatform")
    id("com.squareup.wire")
}

wire {
    kotlin {
        rpcCallStyle = "suspending"
        rpcRole = "server"
        nameSuffix = ""
    }
}

group = "bruhcollective.itaysonlab.ksteam"
version = "1.0-SNAPSHOT"

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }

        withJava()

        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    configure(listOf(
        macosArm64(), macosX64(), iosArm64(), iosX64()
    )) {
        binaries {
            sharedLib {
                baseName = "ksteam_models"
            }
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
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

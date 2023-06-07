plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("build-extensions")
    `maven-publish`
}

group = "bruhcollective.itaysonlab"
version = "r27"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)

    jvm()

    configureOrCreateNativePlatforms(includeApple = true, includeUnix = true, includeMingw = true)

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.1")
                implementation("com.squareup.okio:okio:3.2.0")
                implementation("de.cketti.unicode:kotlin-codepoints-deluxe:0.6.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set("VDF support for kotlinx.serialization")
                description.set("KotlinX Serialization (de)serializer for the Valve Data Format")
                url.set("https://github.com/itaysonlab/ksteam")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://www.opensource.org/licenses/mit-license.php")
                    }
                }

                developers {
                    developer {
                        name.set("iTaysonLab")
                        url.set("https://github.com/itaysonlab/")
                    }
                }
            }
        }
    }
}
plugins {
    id("build-extensions")
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    `maven-publish`
}

group = "bruhcollective.itaysonlab"
version = "r29"

kotlin {
    multiplatformSetup()
}

androidLibrary("bruhcollective.itaysonlab.kotlinx_vdf")

dependencies {
    commonMainImplementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.1")
    commonMainImplementation("com.squareup.okio:okio:3.3.0")
    commonMainImplementation("de.cketti.unicode:kotlin-codepoints-deluxe:0.6.1")
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
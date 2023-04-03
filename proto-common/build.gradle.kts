plugins {
    kotlin("multiplatform")
    id("com.squareup.wire")
    id("build-extensions")
    `maven-publish`
}

wire {
    protoLibrary = true

    kotlin {

    }
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r26"

kotlin {
    jvmToolchain(11)

    jvm()

    configureOrCreateNativePlatforms()

    sourceSets {
        val commonMain by getting
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set("kSteam - Protobufs (Common)")
                description.set("Common Protocol Buffers for kSteam")
                url.set("https://github.com/itaysonlab/ksteam")
            }
        }
    }
}
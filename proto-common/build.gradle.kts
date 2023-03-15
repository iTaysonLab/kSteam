plugins {
    kotlin("jvm")
    id("com.squareup.wire")
    `maven-publish`
}

wire {
    protoLibrary = true

    kotlin {

    }
}

group = "bruhcollective.itaysonlab.ksteam"
version = "r25"

kotlin {
    // jvmToolchain(8)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set("kSteam - Protobufs (Common)")
                description.set("Common Protocol Buffers for kSteam")
                url.set("https://github.com/itaysonlab/ksteam")
                from(components.findByName("java"))
            }
        }
    }
}
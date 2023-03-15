plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `maven-publish`
}

group = "bruhcollective.itaysonlab"
version = "r25"

repositories {
    mavenCentral()
}

kotlin {

}

java {
    withSourcesJar()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.0-RC")
    implementation("com.squareup.okio:okio:3.2.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            pom {
                name.set("kotlinx VDF")
                description.set("KotlinX Serialization serializer support for VDF (Valve Data Format)")
                url.set("https://github.com/itaysonlab/ksteam")
                from(components.findByName("java"))
            }
        }
    }
}
group = "bruhcollective.itaysonlab"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

plugins {
    kotlin("jvm") version "1.7.21" apply false
    kotlin("multiplatform") version "1.7.21" apply false
    kotlin("plugin.serialization") version "1.7.21" apply false
    id("com.squareup.wire") version "4.4.3" apply false
}
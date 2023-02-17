group = "bruhcollective.itaysonlab"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

plugins {
    kotlin("jvm") version "1.8.10" apply false
    kotlin("plugin.serialization") version "1.8.10" apply false
    id("com.squareup.wire") version "4.4.3" apply false
    id("com.android.library") version "7.3.0" apply false
}
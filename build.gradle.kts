group = "bruhcollective.itaysonlab"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}

subprojects {
    apply(plugin = "org.jetbrains.dokka")
}

plugins {
    kotlin("jvm") version "1.8.21" apply false
    kotlin("plugin.serialization") version "1.8.21" apply false
    id("com.squareup.wire") version "4.7.0" apply false
    id("org.jetbrains.dokka") version "1.8.10" apply false
    id("com.android.library") version "8.0.2" apply false
}
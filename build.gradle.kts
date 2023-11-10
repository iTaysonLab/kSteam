group = "bruhcollective.itaysonlab"
version = "1.0-SNAPSHOT"

plugins {
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.jvm) apply false

    alias(libs.plugins.wire) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.android.library) apply false
}

subprojects.onEach {
    it.apply(plugin = "org.jetbrains.dokka")
}
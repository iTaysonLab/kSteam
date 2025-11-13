
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

// For minor edits or local development, this can be set to false in order to skip Kotlin/Native
const val ALLOW_APPLE_PLATFORMS = false

fun KotlinMultiplatformExtension.multiplatformSetup(
    additionalNativeTargetConfig: KotlinNativeTarget.() -> Unit = {}
) {
    // Setup JVM toolchain to 17
    jvmToolchain(17)

    // Enable JVM support
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    // Enable Android support and use JVM 11 target
    androidTarget {
        // jvmToolchain(17)

        publishLibraryVariants("release")
        publishLibraryVariantsGroupedByFlavor = true

        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    if (ALLOW_APPLE_PLATFORMS) {
        // Enable native support for macOS
        macosArm64(additionalNativeTargetConfig)
        macosX64(additionalNativeTargetConfig)

        // Enable iOS support
        iosX64(additionalNativeTargetConfig)
        iosArm64(additionalNativeTargetConfig)
        iosSimulatorArm64(additionalNativeTargetConfig)

        // Allow @ExperimentalObjCName annotation
        sourceSets.all {
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
        }
    }
}

fun KotlinMultiplatformExtension.androidDependencies(
    handler: KotlinDependencyHandler.() -> Unit
) {
   sourceSets["androidMain"].dependencies(handler)
}

fun KotlinMultiplatformExtension.iosDependencies(
    handler: KotlinDependencyHandler.() -> Unit
) {
    sourceSets["iosMain"].dependencies(handler)
}
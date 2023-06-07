import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinMultiplatformExtension.multiplatformSetup(
    additionalNativeTargetConfig: KotlinNativeTarget.() -> Unit = {}
) {
    // Enable Kotlin 1.8.20's new default hierarchy
    targetHierarchy.default()

    // Setup JVM toolchain to 11
    jvmToolchain(11)

    // Enable JVM support
    jvm()

    // Enable Android support and use JVM 11 target
    android {
        jvmToolchain(11)

        publishLibraryVariants("release")
        publishLibraryVariantsGroupedByFlavor = true

        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }

    // Enable iOS support
    iosX64(additionalNativeTargetConfig)
    iosArm64(additionalNativeTargetConfig)
    iosSimulatorArm64(additionalNativeTargetConfig)

    // Enable native support for tvOS
    tvosX64(additionalNativeTargetConfig)
    tvosArm64(additionalNativeTargetConfig)
    tvosSimulatorArm64(additionalNativeTargetConfig)

    // Enable native support for macOS
    macosX64(additionalNativeTargetConfig)
    macosArm64(additionalNativeTargetConfig)

    // Allow @ExperimentalObjCName annotation
    sourceSets.all {
        languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
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
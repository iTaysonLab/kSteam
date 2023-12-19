pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.realm.kotlin") {
                useModule("io.realm.kotlin:gradle-plugin:${requested.version}")
            }
        }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

rootProject.name = "ksteam"

includeBuild("build-extensions")

include(":core")
include(":cli-client")
include(":kotlinx-vdf")

include(":proto-common")

include(":extension-client")
include(":core-persistence")
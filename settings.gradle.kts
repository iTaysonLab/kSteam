pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
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

include(":extension-core")
include(":extension-pics")
include(":extension-guard")
include(":extension-guard-proto-migration")
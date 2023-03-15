rootProject.name = "ksteam"

include(":core")
include(":cli-client")
include(":kotlinx-vdf")

include(":proto-common")

include(":extension-core")
include(":extension-pics")
include(":extension-guard")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}
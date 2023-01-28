rootProject.name = "ksteam"

include(":core")
include(":models")
include(":cli-client")
include(":kotlinx-vdf")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
}
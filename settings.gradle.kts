rootProject.name = "ksteam"

include(":core")
include(":models")
include(":cli-client")

pluginManagement {
    repositories {
        google()
        mavenCentral()
    }
}
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    add("compileOnly", kotlin("gradle-plugin"))
    add("compileOnly", kotlin("gradle-plugin-api"))
}

gradlePlugin {
    plugins {
        create("build-extensions") {
            id = "build-extensions"
            implementationClass = "BuildExtensions"
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10")
    implementation("com.android.library:com.android.library.gradle.plugin:8.1.0")
}
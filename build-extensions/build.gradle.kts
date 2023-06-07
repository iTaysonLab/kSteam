plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    mavenCentral()
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
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.21")
}
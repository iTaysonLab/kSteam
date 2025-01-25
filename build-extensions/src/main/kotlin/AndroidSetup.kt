import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

fun Project.androidLibrary(moduleNamespace: String, extraConfiguration: LibraryExtension.() -> Unit = {}) {
    extensions.configure<LibraryExtension>("android") {
        namespace = moduleNamespace
        compileSdk = 33

        defaultConfig {
            minSdk = 21
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }

        extraConfiguration()
    }
}
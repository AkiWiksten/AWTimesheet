import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AwtimesheetAndroidBaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        plugins.withId("com.android.application") {
            configureAndroidDefaults(extensions.getByType(ApplicationExtension::class.java))
            configureKotlinDefaults()
        }
        plugins.withId("com.android.library") {
            configureAndroidDefaults(extensions.getByType(LibraryExtension::class.java))
            configureKotlinDefaults()
        }
    }
}

private fun configureAndroidDefaults(extension: ApplicationExtension) {
    extension.apply {
        compileSdk = 37

        defaultConfig {
            minSdk = 29
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
}

private fun configureAndroidDefaults(extension: LibraryExtension) {
    extension.apply {
        compileSdk = 37

        defaultConfig {
            minSdk = 29
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
}

private fun Project.configureKotlinDefaults() {
    val kotlinExtension = extensions.findByType(KotlinAndroidProjectExtension::class.java) ?: return
    kotlinExtension.compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}




import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.TestExtension
import org.gradle.api.Plugin
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

private const val DEFAULT_COMPILE_SDK = 37
private const val DEFAULT_MIN_SDK = 29

class AwtimesheetAndroidBaseConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        plugins.withId("com.android.application") {
            configureAndroidDefaults(extensions.getByType(ApplicationExtension::class.java))
            configureKotlinDefaults()
            configureTestDefaults()
        }
        plugins.withId("com.android.library") {
            configureAndroidDefaults(extensions.getByType(LibraryExtension::class.java))
            configureKotlinDefaults()
            configureTestDefaults()
        }
        plugins.withId("com.android.test") {
            configureAndroidDefaults(extensions.getByType(TestExtension::class.java))
            configureKotlinDefaults()
            configureTestDefaults()
        }
    }
}

private fun configureAndroidDefaults(extension: ApplicationExtension) {
    extension.apply {
        compileSdk = DEFAULT_COMPILE_SDK

        defaultConfig {
            minSdk = DEFAULT_MIN_SDK
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
}

private fun configureAndroidDefaults(extension: LibraryExtension) {
    extension.apply {
        compileSdk = DEFAULT_COMPILE_SDK

        defaultConfig {
            minSdk = DEFAULT_MIN_SDK
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
        }
    }
}

private fun configureAndroidDefaults(extension: TestExtension) {
    extension.apply {
        compileSdk = DEFAULT_COMPILE_SDK

        defaultConfig {
            minSdk = DEFAULT_MIN_SDK
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

private fun Project.configureTestDefaults() {
    tasks.withType(Test::class.java).configureEach {
        // Disable upToDateWhen caching for unit tests so they always execute fresh
        // This ensures that test failures are caught even if sources haven't changed
        outputs.upToDateWhen { false }
    }
}

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test

class AwtimesheetAndroidComposeFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("awtimesheet.android.base")
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("com.google.dagger.hilt.android")
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.android.compose.screenshot")

        extensions.configure(LibraryExtension::class.java) {
            buildFeatures {
                compose = true
            }
            experimentalProperties["android.experimental.enableScreenshotTest"] = true
        }

        tasks.withType(Test::class.java).configureEach {
            if (name.contains("ScreenshotTest", ignoreCase = true)) {
                failOnNoDiscoveredTests.set(false)
            }
        }
    }
}


import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AwtimesheetAndroidComposeAppConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("awtimesheet.android.base")
        pluginManager.apply("com.android.application")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        pluginManager.apply("com.google.dagger.hilt.android")
        pluginManager.apply("com.google.devtools.ksp")
        pluginManager.apply("com.android.compose.screenshot")

        extensions.configure(ApplicationExtension::class.java) {
            buildFeatures {
                compose = true
            }
            experimentalProperties["android.experimental.enableScreenshotTest"] = true
        }
    }
}


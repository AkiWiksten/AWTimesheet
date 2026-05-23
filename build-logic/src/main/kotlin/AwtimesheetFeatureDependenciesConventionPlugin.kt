import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

class AwtimesheetFeatureDependenciesConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
        val libs = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

        dependencies.add("implementation", project(":core"))

        dependencies.add("implementation", libs.findLibrary("androidx-core-ktx").get())
        dependencies.add("implementation", libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
        dependencies.add("implementation", dependencies.platform(libs.findLibrary("androidx-compose-bom").get()))
        dependencies.add("implementation", libs.findLibrary("androidx-compose-ui").get())
        dependencies.add("implementation", libs.findLibrary("androidx-compose-material3").get())

        dependencies.add("implementation", libs.findLibrary("hilt-android").get())
        dependencies.add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
        dependencies.add("ksp", libs.findLibrary("hilt-compiler").get())

        dependencies.add("compileOnly", libs.findLibrary("screenshot-validation-api").get())
        dependencies.add("screenshotTestImplementation", libs.findLibrary("screenshot-validation-api").get())
        dependencies.add("screenshotTestImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
        dependencies.add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())

        dependencies.add("testImplementation", libs.findLibrary("junit").get())
        dependencies.add("testImplementation", libs.findLibrary("kotlin-test").get())
        dependencies.add("testImplementation", dependencies.testFixtures(project(":core")))
        }
    }
}


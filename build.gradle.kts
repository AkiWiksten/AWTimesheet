import org.gradle.api.GradleException
import org.gradle.api.artifacts.ProjectDependency

// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
    id("awtimesheet.android.base") apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.screenshot) apply false
}

val verifyModuleBoundaries = tasks.register("verifyModuleBoundaries") {
    notCompatibleWithConfigurationCache("Inspects cross-project dependencies at execution time")

    doLast {
        val violations = mutableListOf<String>()

        subprojects.forEach { module ->
            module.configurations.forEach { cfg ->
                val isTestConfiguration = cfg.name.contains("test", ignoreCase = true)
                cfg.dependencies.withType(ProjectDependency::class.java).forEach { dependency ->
                    val to = dependency.path
                    when {
                        module.path == ":domain" && to == ":data" && !isTestConfiguration -> {
                            violations += "${module.path} -> $to is not allowed on configuration '${cfg.name}'"
                        }

                        module.path.startsWith(":features:") && to == ":data" && !isTestConfiguration -> {
                            violations += "${module.path} -> $to is not allowed on configuration '${cfg.name}'"
                        }
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Module boundary violations detected:")
                    violations.forEach { appendLine(" - $it") }
                }
            )
        }
    }
}



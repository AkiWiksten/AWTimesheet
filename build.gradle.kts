import org.gradle.api.GradleException
import org.gradle.api.artifacts.ProjectDependency
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.kotlin.dsl.configure

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

subprojects {
    pluginManager.apply("io.gitlab.arturbosch.detekt")

    dependencies.add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    }
}

tasks.register("verifyModuleBoundaries") {
    group = "verification"
    description = "Checks forbidden cross-module project dependencies."
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

tasks.register("summarizePerf") {
    group = "verification"
    description = "Alias for :macrobenchmark:summarizePerf"
    dependsOn(":macrobenchmark:summarizePerf")
}

tasks.register("verifyPerf") {
    group = "verification"
    description = "Alias for :macrobenchmark:verifyPerf"
    dependsOn(":macrobenchmark:verifyPerf")
}

tasks.register<Exec>("sequentialBenchmarks") {
    group = "verification"
    description = "Run all macrobenchmarks sequentially with fast startup dataset (one benchmark per invocation)."

    commandLine(
        "powershell",
        "-NoProfile",
        "-NonInteractive",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "$rootDir/macrobenchmark/run_benchmarks_sequential.ps1",
        "-StartupDataset",
        "empty"
    )
}

tasks.register<Exec>("sequentialBenchmarksContinue") {
    group = "verification"
    description = "Run all 10 macrobenchmarks sequentially with fast startup dataset and continue after failures."

    commandLine(
        "powershell",
        "-NoProfile",
        "-NonInteractive",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "$rootDir/macrobenchmark/run_benchmarks_sequential.ps1",
        "-StartupDataset",
        "empty",
        "-ContinueOnFailure"
    )
}

tasks.register<Exec>("sequentialBenchmarksExisting") {
    group = "verification"
    description = "Run all macrobenchmarks sequentially with realistic startup dataset (slower)."

    commandLine(
        "powershell",
        "-NoProfile",
        "-NonInteractive",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "$rootDir/macrobenchmark/run_benchmarks_sequential.ps1",
        "-StartupDataset",
        "existing"
    )
}

tasks.register<Exec>("sequentialBenchmarksEmpty") {
    group = "verification"
    description = "Run all macrobenchmarks sequentially with empty startup dataset profile."

    commandLine(
        "powershell",
        "-NoProfile",
        "-NonInteractive",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "$rootDir/macrobenchmark/run_benchmarks_sequential.ps1",
        "-StartupDataset",
        "empty"
    )
}

tasks.register("sequentialBenchmarksClean") {
    group = "verification"
    description = "Clean macrobenchmark outputs, then run all macrobenchmarks sequentially."
    dependsOn(":macrobenchmark:clean", "sequentialBenchmarks")
}

// Alias that runs all module debug unit tests when user runs ./gradlew testDebugUnitTest
tasks.register("testDebugUnitTest") {
    group = "verification"
    description = "Run all debug unit tests across all modules (aggregated from module-level tasks)."
    dependsOn("testAllDebugUnitTests")
}

// Aggregate unit test tasks from all modules
tasks.register("testAllDebugUnitTests") {
    group = "verification"
    description = "Run all debug unit tests across all modules."
    
    val testTasks = listOf(
        ":core:testDebugUnitTest",
        ":data:testDebugUnitTest",
        ":domain:testDebugUnitTest",
        ":features:calendar:testDebugUnitTest",
        ":features:intro:testDebugUnitTest",
        ":features:projectdetails:testDebugUnitTest",
        ":features:settings:testDebugUnitTest",
        ":features:singleproject:testDebugUnitTest",
        ":features:timesheet:testDebugUnitTest",
        ":features:workday:testDebugUnitTest"
    )
    
    dependsOn(testTasks)
}

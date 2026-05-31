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
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "$rootDir/macrobenchmark/run_benchmarks_sequential.ps1",
        "-StartupDataset",
        "empty",
        "-BenchmarkTimeoutSeconds",
        "360"
    )
}

tasks.register<Exec>("sequentialBenchmarksContinue") {
    group = "verification"
    description = "Run all 10 macrobenchmarks sequentially with fast startup dataset and continue after failures."

    commandLine(
        "powershell",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "$rootDir/macrobenchmark/run_benchmarks_sequential.ps1",
        "-StartupDataset",
        "empty",
        "-BenchmarkTimeoutSeconds",
        "360",
        "-ContinueOnFailure"
    )
}

tasks.register<Exec>("sequentialBenchmarksExisting") {
    group = "verification"
    description = "Run all macrobenchmarks sequentially with realistic startup dataset (slower)."

    commandLine(
        "powershell",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "$rootDir/macrobenchmark/run_benchmarks_sequential.ps1",
        "-StartupDataset",
        "existing",
        "-BenchmarkTimeoutSeconds",
        "360"
    )
}

tasks.register<Exec>("sequentialBenchmarksEmpty") {
    group = "verification"
    description = "Run all macrobenchmarks sequentially with empty startup dataset profile."

    commandLine(
        "powershell",
        "-ExecutionPolicy",
        "Bypass",
        "-File",
        "$rootDir/macrobenchmark/run_benchmarks_sequential.ps1",
        "-StartupDataset",
        "empty",
        "-BenchmarkTimeoutSeconds",
        "360"
    )
}

tasks.register("sequentialBenchmarksClean") {
    group = "verification"
    description = "Clean macrobenchmark outputs, then run all macrobenchmarks sequentially."
    dependsOn(":macrobenchmark:clean", "sequentialBenchmarks")
}



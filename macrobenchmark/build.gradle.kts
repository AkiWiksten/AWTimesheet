plugins {
    id("awtimesheet.android.base")
    id("com.android.test")
}

android {
    namespace = "com.akiwiksten.awtimesheet.macrobenchmark"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    buildTypes {
        create("benchmark") {
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

}

androidComponents {
    beforeVariants(selector().all()) { variant ->
        variant.enable = variant.buildType == "benchmark"
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
}

val pythonExecutable = providers.gradleProperty("perf.pythonExecutable").orElse("python")
val maxJankPercent = providers.gradleProperty("perf.maxJankPercent")
val maxLongFramesPercent = providers.gradleProperty("perf.maxLongFramesPercent")
val benchmarkBuildDir = layout.buildDirectory.get().asFile.absolutePath

tasks.register<Exec>("summarizePerf") {
    group = "verification"
    description = "Summarize macrobenchmark JSON output without pass/fail thresholds."

    commandLine(
        pythonExecutable.get(),
        "-u",
        "$projectDir/tools/summarize_benchmark.py",
        benchmarkBuildDir
    )
}

tasks.register<Exec>("verifyPerf") {
    group = "verification"
    description = "Run connected macrobenchmarks and enforce jank/long-frame thresholds."
    dependsOn("connectedBenchmarkAndroidTest")

    commandLine(
        pythonExecutable.get(),
        "-u",
        "$projectDir/tools/summarize_benchmark.py",
        benchmarkBuildDir,
        "--max-jank-percent",
        maxJankPercent.get(),
        "--max-long-frames-percent",
        maxLongFramesPercent.get(),
        "--fail-on-missing-runs"
    )
}





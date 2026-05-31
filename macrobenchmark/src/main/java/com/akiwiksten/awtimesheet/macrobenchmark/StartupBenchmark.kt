package com.akiwiksten.awtimesheet.macrobenchmark

import android.util.Log
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val STARTUP_BENCHMARK_TAG = "StartupBenchmark"
private const val STARTUP_PROFILE_ARG = "startupProfile"
private const val STARTUP_ITERATIONS_ARG = "startupIterations"
private const val STARTUP_INCLUDE_FRAME_TIMING_ARG = "startupIncludeFrameTiming"
private const val STARTUP_DATASET_ARG = "startupDataset"

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCold() = benchmarkStartup(startupMode = StartupMode.COLD)

    @Test
    fun startupWarm() = benchmarkStartup(startupMode = StartupMode.WARM)

    private fun benchmarkStartup(startupMode: StartupMode) {
        val startupIterations = resolveStartupIterations()
        val startupProfile = resolveStartupProfile()
        val includeFrameTiming = resolveIncludeFrameTiming()
        val startupDataset = resolveStartupDataset()
        var didPrepareDataset = false

        logStartupBenchmarkContext(
            startupMode = startupMode,
            startupIterations = startupIterations,
            startupProfile = startupProfile,
            includeFrameTiming = includeFrameTiming,
            startupDataset = startupDataset
        )

        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = startupMetrics(includeFrameTiming = includeFrameTiming),
            compilationMode = CompilationMode.Partial(),
            startupMode = startupMode,
            iterations = startupIterations,
            setupBlock = {
                if (!didPrepareDataset) {
                    when (startupDataset) {
                        BENCHMARK_SEED_DATASET_EXISTING -> seedRealisticStartupDataIfEmpty()
                        BENCHMARK_SEED_DATASET_EMPTY -> Unit
                    }
                    didPrepareDataset = true
                }
                pressHome()
            }
        ) {
            startActivityAndWait()
        }
    }

    private fun startupMetrics(includeFrameTiming: Boolean) = buildList {
        add(StartupTimingMetric())
        if (includeFrameTiming) {
            add(FrameTimingMetric())
        }
    }

    private fun resolveIncludeFrameTiming(): Boolean {
        val args = InstrumentationRegistry.getArguments()
        return args.getString(STARTUP_INCLUDE_FRAME_TIMING_ARG)
            ?.trim()
            ?.lowercase()
            ?.let { value -> value == "1" || value == "true" || value == "yes" }
            ?: false
    }

    private fun resolveStartupProfile(): String {
        val args = InstrumentationRegistry.getArguments()
        val profile = args.getString(STARTUP_PROFILE_ARG)?.trim()?.lowercase()
        return if (profile == "ci") "ci" else "local"
    }

    private fun resolveStartupDataset(): String {
        val args = InstrumentationRegistry.getArguments()
        val requested = args.getString(STARTUP_DATASET_ARG)?.trim()?.lowercase()
        return when (requested) {
            BENCHMARK_SEED_DATASET_EXISTING -> BENCHMARK_SEED_DATASET_EXISTING
            else -> BENCHMARK_SEED_DATASET_EMPTY // Default to empty for faster local runs
        }
    }

    private fun resolveStartupIterations(): Int {
        val args = InstrumentationRegistry.getArguments()

        // Explicit override always wins when provided.
        val explicitIterations = args.getString(STARTUP_ITERATIONS_ARG)?.toIntOrNull()
        if (explicitIterations != null) {
            return explicitIterations.coerceAtLeast(1)
        }

        return when (resolveStartupProfile()) {
            "ci" -> BenchmarkConfig.STARTUP_ITERATIONS_CI
            else -> BenchmarkConfig.STARTUP_ITERATIONS_LOCAL
        }
    }

    private fun logStartupBenchmarkContext(
        startupMode: StartupMode,
        startupIterations: Int,
        startupProfile: String,
        includeFrameTiming: Boolean,
        startupDataset: String,
    ) {
        val introBypassExpected = BenchmarkConfig.TARGET_PACKAGE.endsWith(".benchmark")
        val message = listOf(
            "startupMode=$startupMode",
            "profile=$startupProfile",
            "dataset=$startupDataset",
            "iterations=$startupIterations",
            "includeFrameTiming=$includeFrameTiming",
            "targetPackage=${BenchmarkConfig.TARGET_PACKAGE}",
            "introBypassExpected=$introBypassExpected"
        ).joinToString(" ")
        Log.i(STARTUP_BENCHMARK_TAG, message)
        // Mirror to stdout so it also appears in test output streams when available.
        println("[$STARTUP_BENCHMARK_TAG] $message")
    }
}

package com.akiwiksten.awtimesheet.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun calScroll() {
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                startActivityAndWait()
                openBottomNavTab(label = TAB_CALENDAR)
                waitForCalendarScreenReady()
                device.waitForIdle()
            }
        ) {
            performVerticalStressScroll()
        }
    }

    @Test
    fun workdayScroll() {
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                startActivityAndWait()
                ensureTargetAppForegroundVisible()
                openBottomNavTab(label = TAB_WORKDAY)
                waitForWorkdayScreenReady()
                device.waitForIdle()
            }
        ) {
            performVerticalStressScroll()
            // Workday can be non-scrollable on empty data; force a deterministic UI transition
            // so FrameTimingMetric always captures renderthread slices.
            openBottomNavTab(label = TAB_SETTINGS)
            waitForSettingsScreenReady()
            openBottomNavTab(label = TAB_WORKDAY)
            device.waitForIdle()
            performVerticalStressScroll()
        }
    }

    @Test
    fun settingsScroll() {
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                startActivityAndWait()
                openBottomNavTab(label = TAB_SETTINGS)
                waitForSettingsScreenReady()
                device.waitForIdle()
            }
        ) {
            performVerticalStressScroll()
        }
    }
}

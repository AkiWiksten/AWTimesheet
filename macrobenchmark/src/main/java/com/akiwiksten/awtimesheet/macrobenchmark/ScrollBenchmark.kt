package com.akiwiksten.awtimesheet.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
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
                openBottomNavTab(label = TAB_WORKDAY)
                waitForWorkdayScreenReady()
                device.waitForIdle()
            }
        ) {
            exerciseWorkdayScrollFlow()
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


private fun MacrobenchmarkScope.exerciseWorkdayScrollFlow() {
    // First measure real workday scrolling.
    performVerticalStressScroll()

    // Open one editable item if present to guarantee frame-producing updates.
    val openedEditor = runCatching {
        clickFirstContentItem(itemDescription = "workday editable item")
        true
    }.getOrDefault(false)

    if (openedEditor) {
        performVerticalStressScroll()
        device.pressBack()
        device.waitForIdle()
    }

    // End on workday and scroll again to keep metric anchored to this screen.
    openBottomNavTab(label = TAB_WORKDAY)
    device.waitForIdle()
    performVerticalStressScroll()
}

private fun MacrobenchmarkScope.clickFirstContentItem(itemDescription: String) {
    repeat(5) { attempt ->
        val contentPoint = device.findObjects(By.clickable(true))
            .asSequence()
            .mapNotNull { node ->
                runCatching {
                    val bounds = node.visibleBounds
                    val centerX = bounds.centerX()
                    val centerY = bounds.centerY()
                    val isInScrollableContentArea =
                        centerY in (device.displayHeight * 0.18f).toInt()..(device.displayHeight * 0.82f).toInt()
                    if (isInScrollableContentArea) centerX to centerY else null
                }.getOrNull()
            }
            .firstOrNull()

        if (contentPoint != null && device.click(contentPoint.first, contentPoint.second)) {
            device.waitForIdle()
            return
        }

        if (attempt < 4) {
            device.waitForIdle()
        }
    }

    error("Could not find a stable clickable $itemDescription")
}

package com.akiwiksten.awtimesheet.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val NAVIGATION_WAIT_MS = 2_000L
private const val CALENDAR_READY_WAIT_MS = 4_000L
private const val SWIPE_STEPS = 24
private const val SWIPE_REPEATS = 6
private const val BOTTOM_NAV_MIN_Y_RATIO = 0.82f
private const val TAB_CALENDAR = "Calendar"
private const val TAB_WORKDAY = "Workday"
private const val TAB_SETTINGS = "Settings"

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
                device.waitForIdle()
            }
        ) {
            performVerticalStressScroll()
        }
    }
}

private fun MacrobenchmarkScope.performVerticalStressScroll() {
    // Repeated long swipes stress LazyColumn measurement/layout and recomposition paths.
    val centerX = device.displayWidth / 2
    val topY = (device.displayHeight * 0.20f).toInt()
    val bottomY = (device.displayHeight * 0.80f).toInt()

    repeat(SWIPE_REPEATS) {
        device.swipe(centerX, bottomY, centerX, topY, SWIPE_STEPS)
        device.waitForIdle()
    }
    repeat(SWIPE_REPEATS) {
        device.swipe(centerX, topY, centerX, bottomY, SWIPE_STEPS)
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.waitForCalendarScreenReady() {
    // Calendar success state renders a read-only text field; wait for it before measuring scroll.
    val ready = device.wait(Until.hasObject(By.clazz("android.widget.EditText")), CALENDAR_READY_WAIT_MS)
    check(ready) {
        "Calendar screen did not reach ready state before scroll measurement."
    }
}

private fun MacrobenchmarkScope.openBottomNavTab(label: String) {
    device.waitForIdle()

    // Primary path: use label text when locale matches benchmark constants.
    val tab = device.wait(Until.findObject(By.text(label)), NAVIGATION_WAIT_MS)
    if (tab != null) {
        tab.click()
        device.waitForIdle()
        return
    }

    // Fallback path: pick a bottom-nav item by its stable order (Calendar, Workday, Settings).
    val targetIndex = when (label) {
        TAB_CALENDAR -> 0
        TAB_WORKDAY -> 1
        TAB_SETTINGS -> 2
        else -> null
    }

    if (targetIndex != null) {
        repeat(3) {
            val bottomNavCandidates = device.findObjects(By.clickable(true))
                .asSequence()
                .mapNotNull { node ->
                    runCatching {
                        val bounds = node.visibleBounds
                        val centerY = bounds.centerY()
                        if (centerY >= (device.displayHeight * BOTTOM_NAV_MIN_Y_RATIO).toInt()) {
                            bounds.centerX() to centerY
                        } else {
                            null
                        }
                    }.getOrNull()
                }
                .sortedBy { it.first }
                .toList()

            if (bottomNavCandidates.size >= 3) {
                val (x, y) = bottomNavCandidates[targetIndex]
                if (device.click(x, y)) {
                    device.waitForIdle()
                    return
                }
            }

            device.waitForIdle()
        }
    }

    error("Could not find bottom nav tab '$label'.")
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

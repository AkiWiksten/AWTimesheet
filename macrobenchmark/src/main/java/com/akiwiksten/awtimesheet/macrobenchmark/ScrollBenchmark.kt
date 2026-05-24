package com.akiwiksten.awtimesheet.macrobenchmark

import android.content.Intent
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
private const val SWIPE_STEPS = 24
private const val SWIPE_REPEATS = 6
private const val TAB_WORKDAY = "Workday"
private const val TAB_SETTINGS = "Settings"

@RunWith(AndroidJUnit4::class)
class ScrollBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun calendarScrollFrameTiming() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = 2,
            setupBlock = {
                startActivityAndWait()
                dismissIntroScreenIfVisible()
            }
        ) {
            performVerticalStressScroll()
        }
    }

    @Test
    fun workdayScrollFrameTiming() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = 2,
            setupBlock = {
                startActivityAndWait()
                dismissIntroScreenIfVisible()
                openBottomNavTab(label = TAB_WORKDAY)
            }
        ) {
            performVerticalStressScroll()
        }
    }

    @Test
    fun settingsScrollFrameTiming() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = 2,
            setupBlock = {
                startActivityAndWait()
                dismissIntroScreenIfVisible()
                openBottomNavTab(label = TAB_SETTINGS)
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

private fun MacrobenchmarkScope.openBottomNavTab(label: String) {
    val tab = device.wait(Until.findObject(By.text(label)), NAVIGATION_WAIT_MS)
        ?: error("Could not find bottom nav tab '$label'.")
    tab.click()
    device.waitForIdle()
}




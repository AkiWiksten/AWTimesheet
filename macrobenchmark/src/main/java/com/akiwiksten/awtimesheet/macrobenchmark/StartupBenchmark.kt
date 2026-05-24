package com.akiwiksten.awtimesheet.macrobenchmark

import android.content.Intent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

internal val TARGET_PACKAGE = "com.akiwiksten.awtimesheet.benchmark"

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun startupCold() = benchmarkStartup(startupMode = StartupMode.COLD)

    @Test
    fun startupWarm() = benchmarkStartup(startupMode = StartupMode.WARM)

    private fun benchmarkStartup(startupMode: StartupMode) {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(
                StartupTimingMetric(),
                FrameTimingMetric()
            ),
            compilationMode = CompilationMode.Partial(),
            startupMode = startupMode,
            iterations = 2,
            setupBlock = {
                pressHome()
            }
        ) {
            startActivityAndWait()
            dismissIntroScreenIfVisible()
        }
    }
}

internal fun MacrobenchmarkScope.dismissIntroScreenIfVisible() {
    val centerX = device.displayWidth / 2
    val centerY = device.displayHeight / 2
    device.click(centerX, centerY)
    device.waitForIdle()
}



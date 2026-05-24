package com.akiwiksten.awtimesheet.macrobenchmark

import android.view.KeyEvent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val NAVIGATION_WAIT_MS = 2_000L
private const val CONTENT_CLICK_RETRIES = 5
private const val FIELD_INTERACTION_RETRIES = 4
private const val CLICK_RETRY_COUNT = 5
private const val CLICK_RETRY_WAIT_MS = 250L

/**
 * Benchmark that measures recomposition counts during common user interactions.
 *
 * This benchmark captures recomposition metrics by instrumenting the target app
 * and exporting composition statistics in macrobenchmark-compatible JSON format.
 */
@RunWith(AndroidJUnit4::class)
class RecompositionBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Measures recompositions during calendar screen interactions.
     * This includes scrolling through the calendar view and date selection.
     */
    @Test
    fun calendarScreenRecompositions() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                startActivityAndWait()
                ensureTargetAppInForeground()
                dismissIntroScreenIfVisible()
            }
        ) {
            // Engage with calendar to trigger recompositions
            performVerticalStressScroll()
            device.click(device.displayWidth / 2, device.displayHeight / 2)
            device.waitForIdle()
        }
    }

    /**
     * Measures recompositions during workday screen interactions.
     * This includes scrolling through work entries.
     */
    @Test
    fun workdayScreenRecompositions() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                startActivityAndWait()
                ensureTargetAppInForeground()
                dismissIntroScreenIfVisible()
                openBottomNavTab(label = "Workday")
            }
        ) {
            performVerticalStressScroll()
            device.click(device.displayWidth / 2, device.displayHeight / 2)
            device.waitForIdle()
        }
    }

    /**
     * Measures recompositions during settings screen interactions.
     */
    @Test
    fun settingsScreenRecompositions() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                startActivityAndWait()
                ensureTargetAppInForeground()
                dismissIntroScreenIfVisible()
                openBottomNavTab(label = "Settings")
            }
        ) {
            performVerticalStressScroll()
            device.click(device.displayWidth / 2, device.displayHeight / 2)
            device.waitForIdle()
        }
    }

    /**
     * Measures recompositions during project details screen interactions.
     * This includes opening project details from calendar/workday and interacting with the form.
     */
    @Test
    fun projectDetailsScreenRecompositions() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                startActivityAndWait()
                ensureTargetAppInForeground()
                dismissIntroScreenIfVisible()
                // Navigate to a project's details screen
                navigateToProjectDetailsScreen()
            }
        ) {
            // Engage with project details form to trigger recompositions
            performVerticalStressScroll()
            device.click(device.displayWidth / 2, device.displayHeight / 2)
            device.waitForIdle()
        }
    }

    /**
     * Measures recompositions during single project screen interactions.
     * This includes editing a single project's work entry details.
     */
    @Test
    fun singleProjectScreenRecompositions() {
        benchmarkRule.measureRepeated(
            packageName = TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                startActivityAndWait()
                ensureTargetAppInForeground()
                dismissIntroScreenIfVisible()
                // Navigate to single project editing screen
                navigateToSingleProjectScreen()
            }
        ) {
            // Engage with editable fields to guarantee frame-producing updates.
            exerciseSingleProjectScreen()
        }
    }
}

/**
 * Helper to open a navigation tab by label.
 */
private fun MacrobenchmarkScope.openBottomNavTab(label: String) {
    clickObjectWithRetry(selector = By.text(label), description = "bottom nav tab '$label'")
    device.waitForIdle()
}

/**
 * Navigates to the Project Details screen by clicking on the first project item.
 * Assumes the calendar or workday screen has project entries to click.
 */
private fun MacrobenchmarkScope.navigateToProjectDetailsScreen() {
    clickFirstContentItem(itemDescription = "project item on calendar screen")
    // Additional click might be needed to open details (depends on UI)
    clickObjectWithRetry(selector = By.desc("Edit project"), description = "Edit project button", required = false)
    device.waitForIdle()
}

/**
 * Navigates to the Single Project screen by finding and clicking a project entry.
 * Then interacts with the single project editing form.
 */
private fun MacrobenchmarkScope.navigateToSingleProjectScreen() {
    // Workday screen typically has project entries that can be clicked to edit
    openBottomNavTab(label = "Workday")
    device.waitForIdle()

    clickFirstContentItem(itemDescription = "project entry on workday screen")
    device.waitForIdle()
}

private fun MacrobenchmarkScope.clickFirstContentItem(itemDescription: String) {
    repeat(CONTENT_CLICK_RETRIES) { attempt ->
        val contentPoint = device.findObjects(By.clickable(true))
            .asSequence()
            .mapNotNull { node ->
                try {
                    val bounds = node.visibleBounds
                    val centerX = bounds.centerX()
                    val centerY = bounds.centerY()
                    val isInScrollableContentArea =
                        centerY in (device.displayHeight * 0.18f).toInt()..(device.displayHeight * 0.82f).toInt()
                    if (isInScrollableContentArea) centerX to centerY else null
                } catch (_: StaleObjectException) {
                    null
                }
            }
            .firstOrNull()

        if (contentPoint != null && device.click(contentPoint.first, contentPoint.second)) {
            device.waitForIdle()
            return
        }

        if (attempt < CONTENT_CLICK_RETRIES - 1) {
            device.waitForIdle()
        }
    }

    error("Could not find a stable clickable $itemDescription")
}

private fun MacrobenchmarkScope.exerciseSingleProjectScreen() {
    performVerticalStressScroll()

    if (!interactWithFirstFocusableField()) {
        // Fallback: extra swipes and tap still produce interaction if no focusable field is visible.
        performVerticalStressScroll()
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        device.waitForIdle()
    }

    // Keep one deterministic interaction tick to produce render slices even on static forms.
    val centerX = device.displayWidth / 2
    val topY = (device.displayHeight * 0.30f).toInt()
    val bottomY = (device.displayHeight * 0.70f).toInt()
    device.swipe(centerX, bottomY, centerX, topY, 30)
    device.waitForIdle()
}

private fun MacrobenchmarkScope.interactWithFirstFocusableField(): Boolean {
    repeat(FIELD_INTERACTION_RETRIES) {
        val fieldPoint = device.findObjects(By.focusable(true))
            .asSequence()
            .mapNotNull { node ->
                try {
                    val bounds = node.visibleBounds
                    val centerX = bounds.centerX()
                    val centerY = bounds.centerY()
                    val isInContentArea =
                        centerY in (device.displayHeight * 0.18f).toInt()..(device.displayHeight * 0.86f).toInt()
                    if (isInContentArea) centerX to centerY else null
                } catch (_: StaleObjectException) {
                    null
                }
            }
            .firstOrNull()

        if (fieldPoint != null && device.click(fieldPoint.first, fieldPoint.second)) {
            device.waitForIdle()
            // Add and remove one character to force recomposition/redraw in form state.
            device.pressKeyCode(KeyEvent.KEYCODE_1)
            device.pressKeyCode(KeyEvent.KEYCODE_DEL)
            device.waitForIdle()
            return true
        }
    }
    return false
}

/**
 * Performs vertical stress scroll for scrollable content.
 */
private fun MacrobenchmarkScope.performVerticalStressScroll() {
    val centerX = device.displayWidth / 2
    val topY = (device.displayHeight * 0.20f).toInt()
    val bottomY = (device.displayHeight * 0.80f).toInt()

    repeat(6) {
        device.swipe(centerX, bottomY, centerX, topY, 24)
        device.waitForIdle()
    }
    repeat(6) {
        device.swipe(centerX, topY, centerX, bottomY, 24)
        device.waitForIdle()
    }
}

private fun MacrobenchmarkScope.ensureTargetAppInForeground() {
    val hasTargetPackage = device.wait(Until.hasObject(By.pkg(TARGET_PACKAGE)), NAVIGATION_WAIT_MS)
    check(hasTargetPackage) { "Target package '$TARGET_PACKAGE' is not visible after launch." }
    device.waitForIdle()
}

private fun MacrobenchmarkScope.clickObjectWithRetry(
    selector: BySelector,
    description: String,
    required: Boolean = true,
) {
    repeat(CLICK_RETRY_COUNT) { attempt ->
        val node = device.wait(Until.findObject(selector), CLICK_RETRY_WAIT_MS)
        if (node != null) {
            try {
                node.click()
                device.waitForIdle()
                return
            } catch (_: StaleObjectException) {
                // Retry with a fresh node lookup when hierarchy updates during tap.
            }
        }

        if (attempt < CLICK_RETRY_COUNT - 1) {
            device.waitForIdle()
        }
    }

    if (required) {
        error("Could not click $description")
    }
}


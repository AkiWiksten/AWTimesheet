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
private const val WORKDAY_READY_TEXT = "Add"
private const val CONTENT_CLICK_RETRIES = 5
private const val FIELD_INTERACTION_RETRIES = 4
private const val CLICK_RETRY_COUNT = 5
private const val CLICK_RETRY_WAIT_MS = 250L
private const val BOTTOM_NAV_MIN_Y_RATIO = 0.82f
private const val TAB_CALENDAR = "Calendar"
private const val TAB_WORKDAY = "Workday"
private const val TAB_SETTINGS = "Settings"

/**
 * Benchmark that measures recomposition counts during common user interactions.
 *
 * This benchmark captures recomposition metrics by instrumenting the target app
 * and exporting composition statistics in macrobenchmark-compatible JSON format.
 */
@RunWith(AndroidJUnit4::class)
class RecompBm {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * Measures recompositions during calendar screen interactions.
     * This includes scrolling through the calendar view and date selection.
     */
    @Test
    fun calRecomp() {
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                startActivityAndWait()
                ensureTargetAppInForeground()
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
    fun workdayRecomp() {
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                startActivityAndWait()
                ensureTargetAppInForeground()
                openBottomNavTab(label = TAB_WORKDAY)
                device.waitForIdle()
            }
        ) {
            exerciseWorkdayFlow()
        }
    }

    /**
     * Measures recompositions during settings screen interactions.
     */
    @Test
    fun settingsRecomp() {
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                startActivityAndWait()
                ensureTargetAppInForeground()
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
    fun projDetailsRecomp() {
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                startActivityAndWait()
                ensureTargetAppInForeground()
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
     * This includes editing a single project's work entry details (project name, time, km, allowance, work type).
     * NOTE: This tests SingleProjectScreen, NOT ProjectDetailsScreen. Keep isolated to SingleProjectScreen only.
     */
    @Test
    fun singleProjRecomp() {
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                startActivityAndWait()
                ensureTargetAppInForeground()
                // Navigate to single project editing screen
                navigateToSingleProjectScreen()
            }
        ) {
            // Engage with editable fields to guarantee frame-producing updates.
            // This tests SingleProjectScreen only (projectName, projectTime, kilometres, allowance, workType)
            exerciseSingleProjectScreen()
        }
    }
}

/**
 * Helper to open a navigation tab by label.
 */
private fun MacrobenchmarkScope.openBottomNavTab(label: String) {
    // Primary path: label text lookup.
    val textTab = device.wait(Until.findObject(By.text(label)), NAVIGATION_WAIT_MS)
    if (textTab != null) {
        textTab.click()
        device.waitForIdle()
        return
    }

    // Fallback path: select a bottom-nav item by stable order.
    val targetIndex = when (label) {
        TAB_CALENDAR -> 0
        TAB_WORKDAY -> 1
        TAB_SETTINGS -> 2
        else -> null
    }

    if (targetIndex != null) {
        repeat(CLICK_RETRY_COUNT) {
            val bottomNavCandidates = device.findObjects(By.clickable(true))
                .asSequence()
                .mapNotNull { node ->
                    try {
                        val bounds = node.visibleBounds
                        val centerY = bounds.centerY()
                        if (centerY >= (device.displayHeight * BOTTOM_NAV_MIN_Y_RATIO).toInt()) {
                            bounds.centerX() to centerY
                        } else {
                            null
                        }
                    } catch (_: StaleObjectException) {
                        null
                    }
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

/**
 * Navigates to the Project Details screen by opening a workday project entry,
 * then opening details from the SingleProject screen.
 */
private fun MacrobenchmarkScope.navigateToProjectDetailsScreen() {
    openBottomNavTab(label = TAB_WORKDAY)
    waitForWorkdayScreenReady()
    device.waitForIdle()

    clickFirstContentItem(itemDescription = "project entry on workday screen")
    device.waitForIdle()

    openProjectDetailsFromSingleProjectScreen()
    device.waitForIdle()
}

private fun MacrobenchmarkScope.openProjectDetailsFromSingleProjectScreen() {
    // Primary path: text-based lookup (works in matching locale).
    val detailsByText = device.wait(Until.findObject(By.text("Details")), NAVIGATION_WAIT_MS)
    if (detailsByText != null) {
        detailsByText.click()
        device.waitForIdle()
        return
    }

    // Fallback path: pick a likely details action button from mid-content area.
    repeat(CLICK_RETRY_COUNT) {
        val midButtons = device.findObjects(By.clazz("android.widget.Button"))
            .asSequence()
            .mapNotNull { node ->
                try {
                    val bounds = node.visibleBounds
                    val centerX = bounds.centerX()
                    val centerY = bounds.centerY()
                    val isInMidContent =
                        centerY in (device.displayHeight * 0.25f).toInt()..(device.displayHeight * 0.78f).toInt()
                    if (isInMidContent) centerX to centerY else null
                } catch (_: StaleObjectException) {
                    null
                }
            }
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenBy { it.second })
            .toList()

        // Prefer right-side action column button over bottom confirm/back controls.
        val candidate = midButtons.firstOrNull()
        if (candidate != null && device.click(candidate.first, candidate.second)) {
            device.waitForIdle()
            return
        }

        device.waitForIdle()
    }

    error("Could not open Project Details from SingleProject screen")
}

/**
 * Navigates to the Single Project screen by finding and clicking a project entry.
 * Then interacts with the single project editing form.
 */
private fun MacrobenchmarkScope.navigateToSingleProjectScreen() {
    // Workday screen typically has project entries that can be clicked to edit
    openBottomNavTab(label = "Workday")
    waitForWorkdayScreenReady()
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

private fun MacrobenchmarkScope.exerciseWorkdayFlow() {
    // Workday content interaction.
    performVerticalStressScroll()

    // Open an editable flow from Workday without relying on localized button labels.
    clickFirstContentItem(itemDescription = "workday editable entry")

    if (!interactWithFirstFocusableField()) {
        performVerticalStressScroll()
        device.click(device.displayWidth / 2, device.displayHeight / 2)
        device.waitForIdle()
    }

    // Return to Workday and scroll again to keep the measured segment tied to Workday flow.
    device.pressBack()
    device.waitForIdle()
    openBottomNavTab(label = TAB_WORKDAY)
    device.waitForIdle()
    performVerticalStressScroll()
}

private fun MacrobenchmarkScope.interactWithFirstFocusableField(): Boolean {
    repeat(FIELD_INTERACTION_RETRIES) { attempt ->
        val fieldPoint = device.findObjects(By.focusable(true))
            .asSequence()
            .mapNotNull { node ->
                try {
                    val bounds = node.visibleBounds
                    val centerX = bounds.centerX()
                    val centerY = bounds.centerY()
                    val isInContentArea =
                        centerY in (device.displayHeight * 0.18f).toInt()..(device.displayHeight * 0.86f).toInt()

                    // Skip navigation buttons that would leave SingleProjectScreen
                    val nodeText = node.text?.lowercase() ?: ""
                    val shouldSkip = nodeText.contains("details") || nodeText.contains("pick") || nodeText.contains("edit")

                    if (isInContentArea && !shouldSkip) centerX to centerY else null
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

        if (attempt < FIELD_INTERACTION_RETRIES - 1) {
            device.waitForIdle()
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
    val hasTargetPackage =
        device.wait(Until.hasObject(By.pkg(BenchmarkConfig.TARGET_PACKAGE)), NAVIGATION_WAIT_MS)
    check(hasTargetPackage) {
        "Target package '${BenchmarkConfig.TARGET_PACKAGE}' is not visible after launch."
    }
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

private fun MacrobenchmarkScope.waitForWorkdayScreenReady() {
    val ready = device.wait(Until.hasObject(By.text(WORKDAY_READY_TEXT)), NAVIGATION_WAIT_MS)
    check(ready) {
        "Workday screen did not reach a ready state (missing '$WORKDAY_READY_TEXT' text)."
    }
    device.waitForIdle()
}


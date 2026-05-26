package com.akiwiksten.awtimesheet.macrobenchmark

import android.view.KeyEvent
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Shared constants (NAVIGATION_WAIT_MS, CALENDAR_READY_WAIT_MS, WORKDAY_READY_TEXT,
// INTRO_DISMISS_TAPS/WAIT_MS, BOTTOM_NAV_MIN_Y_RATIO, TAB_*) live in BenchmarkHelpers.kt.
private const val FIELD_INTERACTION_RETRIES = 4
private const val CLICK_RETRY_COUNT = 5
private const val CLICK_RETRY_WAIT_MS = 250L
private const val CONTENT_AREA_MIN_Y_RATIO = 0.18f
private const val CONTENT_AREA_MAX_Y_RATIO = 0.86f
private const val MIN_EXPECTED_DETAILS_FOCUSABLES = 2
private const val MAX_DEBUG_TEXT_TOKENS = 8
private val TRANSIENT_DIALOG_ACTION_TEXTS = listOf("Dismiss", "Cancel", "OK", "Confirm")

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
                openBottomNavTab(label = TAB_CALENDAR)
                waitForCalendarScreenReady()
                device.waitForIdle()
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
                waitForWorkdayScreenReady()
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
                openBottomNavTab(label = TAB_SETTINGS)
                waitForSettingsScreenReady()
                device.waitForIdle()
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
            exerciseProjectDetailsFlow()
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

// openBottomNavTab, dismissIntroIfPresent, waitForXxxScreenReady, and
// performVerticalStressScroll are defined in BenchmarkHelpers.kt.

/**
 * Navigates to the Project Details screen by adding a new project (via "Add" button),
 * then opening details from the SingleProject screen.
 *
 * If the workday has no projects, we add one via the "Add" button which opens
 * SingleProjectScreen directly with an empty project. Then we click "Details"
 * to reach ProjectDetailsScreen.
 */
private fun MacrobenchmarkScope.navigateToProjectDetailsScreen() {
    openBottomNavTab(label = TAB_WORKDAY)
    waitForWorkdayScreenReady()
    device.waitForIdle()

    // Click "Add" button to create a new project entry and open SingleProjectScreen
    clickWorkdayAddButton()
    device.waitForIdle()

    // Now we're on SingleProjectScreen; navigate to ProjectDetailsScreen
    openProjectDetailsFromSingleProjectScreen()
    device.waitForIdle()
    requireLikelyProjectDetailsScreen(context = "after opening Project Details")
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
 * Navigates to the Single Project screen via Workday's Add action.
 *
 * Using generic content clicks on Workday can hit the stats-card clock picker.
 * The Add button is deterministic and opens SingleProjectScreen directly.
 */
private fun MacrobenchmarkScope.navigateToSingleProjectScreen() {
    openBottomNavTab(label = TAB_WORKDAY)
    waitForWorkdayScreenReady()
    device.waitForIdle()

    clickWorkdayAddButton()
    device.waitForIdle()
    waitForSingleProjectScreenReady()
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
    // Keep measured actions on Workday only; avoid route transitions that can
    // inflate frame-overrun without reflecting Workday recomposition cost.
    performVerticalStressScroll()
    device.click(device.displayWidth / 2, (device.displayHeight * 0.45f).toInt())
    device.waitForIdle()
    performVerticalStressScroll()
}

private fun MacrobenchmarkScope.exerciseProjectDetailsFlow() {
    ensureTargetAppInForeground()

    // Stay on ProjectDetails and drive deterministic form interactions in-place.
    requireLikelyProjectDetailsScreen(context = "during measured project-details flow")

    if (!interactWithFirstFocusableField()) {
        // Fallback interaction when no focusable input is currently visible.
        val centerX = device.displayWidth / 2
        val topY = (device.displayHeight * 0.30f).toInt()
        val bottomY = (device.displayHeight * 0.70f).toInt()
        device.swipe(centerX, bottomY, centerX, topY, 30)
        device.waitForIdle()
        device.click(centerX, (device.displayHeight * 0.50f).toInt())
        device.waitForIdle()
    }

    closeTransientDialogIfPresent()

    performVerticalStressScroll()
}

private fun MacrobenchmarkScope.requireLikelyProjectDetailsScreen(context: String) {
    val minContentY = (device.displayHeight * CONTENT_AREA_MIN_Y_RATIO).toInt()
    val maxContentY = (device.displayHeight * CONTENT_AREA_MAX_Y_RATIO).toInt()

    fun countContentFocusableNodes(): Int =
        device.findObjects(By.focusable(true))
            .asSequence()
            .count { node ->
                try {
                    val centerY = node.visibleBounds.centerY()
                    centerY in minContentY..maxContentY
                } catch (_: StaleObjectException) {
                    false
                }
            }

    var contentFocusableCount = countContentFocusableNodes()

    // Dialogs (time/date pickers) can temporarily hide focusable fields on ProjectDetails.
    if (contentFocusableCount < MIN_EXPECTED_DETAILS_FOCUSABLES && closeTransientDialogIfPresent()) {
        device.waitForIdle()
        contentFocusableCount = countContentFocusableNodes()
    }

    if (contentFocusableCount >= MIN_EXPECTED_DETAILS_FOCUSABLES) {
        return
    }

    val textSnapshot = device.findObjects(By.clazz("android.widget.TextView"))
        .asSequence()
        .mapNotNull { node ->
            runCatching { node.text?.trim() }
                .getOrNull()
                ?.takeIf { it.isNotEmpty() }
        }
        .distinct()
        .take(MAX_DEBUG_TEXT_TOKENS)
        .joinToString(separator = " | ")

    error(
        "Expected ProjectDetails-like screen $context, but found only " +
            "$contentFocusableCount content focusable nodes. " +
            "Visible texts snapshot: [$textSnapshot]"
    )
}

private fun MacrobenchmarkScope.closeTransientDialogIfPresent(): Boolean {
    TRANSIENT_DIALOG_ACTION_TEXTS.forEach { actionText ->
        val action = device.wait(Until.findObject(By.text(actionText)), CLICK_RETRY_WAIT_MS)
        if (action != null) {
            action.click()
            device.waitForIdle()
            return true
        }
    }
    return false
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
                        centerY in (device.displayHeight * CONTENT_AREA_MIN_Y_RATIO).toInt()..
                            (device.displayHeight * CONTENT_AREA_MAX_Y_RATIO).toInt()

                    // Skip navigation buttons that would leave SingleProjectScreen
                    val nodeText = node.text?.lowercase() ?: ""
                    val shouldSkip = nodeText.contains("details")
                        || nodeText.contains("pick")
                        || nodeText.contains("edit")

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

private fun MacrobenchmarkScope.ensureTargetAppInForeground() {
    val hasTargetPackage =
        device.wait(Until.hasObject(By.pkg(BenchmarkConfig.TARGET_PACKAGE)), NAVIGATION_WAIT_MS)
    check(hasTargetPackage) {
        "Target package '${BenchmarkConfig.TARGET_PACKAGE}' is not visible after launch."
    }
    device.waitForIdle()
}

/**
 * Clicks the Workday "Add" button using locale-aware text matching
 * (English: "Add", Finnish: "Lisää", Swedish: "Lägg till").
 */
private fun MacrobenchmarkScope.clickWorkdayAddButton() {
    repeat(CLICK_RETRY_COUNT) { attempt ->
        val node = WORKDAY_READY_TEXTS
            .firstNotNullOfOrNull { text ->
                device.wait(Until.findObject(By.text(text)), CLICK_RETRY_WAIT_MS)
            }
        if (node != null) {
            try {
                node.click()
                device.waitForIdle()
                return
            } catch (_: StaleObjectException) {
                // Retry on stale hierarchy.
            }
        }
        if (attempt < CLICK_RETRY_COUNT - 1) {
            device.waitForIdle()
        }
    }
    error("Could not click Add button on WorkdayScreen (tried: ${WORKDAY_READY_TEXTS.joinToString()})")
}



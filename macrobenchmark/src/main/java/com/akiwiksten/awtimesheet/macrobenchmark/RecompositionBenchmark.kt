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
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Shared constants (NAVIGATION_WAIT_MS, CALENDAR_READY_WAIT_MS, WORKDAY_READY_TEXT,
// INTRO_DISMISS_TAPS/WAIT_MS, BOTTOM_NAV_MIN_Y_RATIO, TAB_*) live in BenchmarkHelpers.kt.
private const val FIELD_INTERACTION_RETRIES = 4
private const val CLICK_RETRY_COUNT = 5
private const val CLICK_RETRY_WAIT_MS = 250L
private const val MAX_NAV_DEBUG_TOKENS = 12
private const val CONTENT_AREA_MIN_Y_RATIO = 0.18f
private const val CONTENT_AREA_MAX_Y_RATIO = 0.86f
private const val MIN_EXPECTED_DETAILS_FOCUSABLES = 2
private const val MAX_DEBUG_TEXT_TOKENS = 8
private val TRANSIENT_DIALOG_ACTION_TEXTS = listOf("Dismiss", "Cancel", "OK", "Confirm")
private val WORKDAY_ADD_ACTION_TEXTS = listOf("Add", "Lisää", "Lägg till")
private val WORKDAY_EDIT_ACTION_TEXTS = listOf("Edit", "Muokkaa", "Redigera")

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
        var didPrepareDataset = false
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                if (!didPrepareDataset) {
                    seedRealisticStartupDataIfEmpty()
                    didPrepareDataset = true
                }
                startActivityAndWait()
                openBottomNavTab(label = TAB_CALENDAR)
                waitForCalendarScreenReady()
                device.waitForIdle()
            }
        ) {
            // Engage with calendar to trigger recompositions
            performVerticalInteractionScroll()
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
        var didPrepareDataset = false
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                if (!didPrepareDataset) {
                    seedRealisticStartupDataIfEmpty()
                    didPrepareDataset = true
                }
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
        var didPrepareDataset = false
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                if (!didPrepareDataset) {
                    seedRealisticStartupDataIfEmpty()
                    didPrepareDataset = true
                }
                startActivityAndWait()
                ensureTargetAppInForeground()
                openBottomNavTab(label = TAB_SETTINGS)
                waitForSettingsScreenReady()
                device.waitForIdle()
            }
        ) {
            performVerticalInteractionScroll()
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
        var didPrepareDataset = false
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                if (!didPrepareDataset) {
                    seedRealisticStartupDataIfEmpty()
                    didPrepareDataset = true
                }
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
        var didPrepareDataset = false
        benchmarkRule.measureRepeated(
            packageName = BenchmarkConfig.TARGET_PACKAGE,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = BenchmarkConfig.ITERATIONS,
            setupBlock = {
                if (!didPrepareDataset) {
                    seedRealisticStartupDataIfEmpty()
                    didPrepareDataset = true
                }
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
// performVerticalInteractionScroll are defined in BenchmarkHelpers.kt.

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
    waitForSingleProjectScreenReady()

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
    performVerticalInteractionScroll()

    if (!interactWithFirstFocusableField()) {
        // Fallback: extra swipes and tap still produce interaction if no focusable field is visible.
        performVerticalInteractionScroll()
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
    performVerticalInteractionScroll()
    device.click(device.displayWidth / 2, (device.displayHeight * 0.45f).toInt())
    device.waitForIdle()
    performVerticalInteractionScroll()
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

    performVerticalInteractionScroll()
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
        if (attempt == 0) {
            ensureWorkdayAddActionVisible()
        } else {
            scrollTowardWorkdayActionButtons()
            ensureWorkdayAddActionVisible()
        }

        val node = WORKDAY_ADD_ACTION_TEXTS
            .firstNotNullOfOrNull { text ->
                device.wait(Until.findObject(By.text(text)), CLICK_RETRY_WAIT_MS)
            }
        if (node != null) {
            try {
                if (clickNodeViaClickableAncestor(node)) {
                    device.waitForIdle()
                    if (isLikelySingleProjectScreenVisible()) {
                        return
                    }
                    closeTransientDialogIfPresent()
                }
            } catch (_: StaleObjectException) {
                // Retry on stale hierarchy.
            }
        }

        // Fallback: left-most button in Workday action row (Add/Edit/Delete).
        val addActionX = (device.displayWidth * 0.18f).toInt()
        val addActionYCandidates = listOf(0.76f, 0.72f, 0.68f)
            .map { ratio -> (device.displayHeight * ratio).toInt() }
        addActionYCandidates.forEach { y ->
            if (device.click(addActionX, y)) {
                device.waitForIdle()
                if (isLikelySingleProjectScreenVisible()) {
                    return
                }
                closeTransientDialogIfPresent()
            }
        }

        val fallbackPoint = device.findObjects(By.clickable(true))
            .asSequence()
            .mapNotNull { candidate ->
                try {
                    val bounds = candidate.visibleBounds
                    val centerX = bounds.centerX()
                    val centerY = bounds.centerY()
                    val isLikelyAddAction =
                        centerX >= (device.displayWidth * 0.52f).toInt() &&
                            centerY in (device.displayHeight * 0.25f).toInt()..
                            (device.displayHeight * 0.78f).toInt()
                    if (isLikelyAddAction) centerX to centerY else null
                } catch (_: StaleObjectException) {
                    null
                }
            }
            .sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenBy { it.second })
            .firstOrNull()

        if (fallbackPoint != null && device.click(fallbackPoint.first, fallbackPoint.second)) {
            device.waitForIdle()
            if (isLikelySingleProjectScreenVisible()) {
                return
            }
            closeTransientDialogIfPresent()
        }

        if (attemptOpenSingleProjectViaEditAction()) {
            return
        }

        if (attempt < CLICK_RETRY_COUNT - 1) {
            device.waitForIdle()
        }
    }
    error(
        "Could not open SingleProject screen from Workday Add action " +
            "(tried: ${WORKDAY_ADD_ACTION_TEXTS.joinToString()}). " +
            "UI snapshot: ${buildWorkdayNavigationDebugSnapshot()}"
    )
}

private fun MacrobenchmarkScope.isLikelySingleProjectScreenVisible(): Boolean {
    val fieldCount = device.findObjects(By.clazz("android.widget.EditText")).size
    val hasActionLabel = SINGLE_PROJECT_READY_TEXTS.any { label ->
        device.hasObject(By.text(label))
    }
    return fieldCount >= 1 && hasActionLabel
}

private fun MacrobenchmarkScope.scrollTowardWorkdayActionButtons() {
    val centerX = device.displayWidth / 2
    val startY = (device.displayHeight * 0.78f).toInt()
    val endY = (device.displayHeight * 0.30f).toInt()
    device.swipe(centerX, startY, centerX, endY, 24)
    device.waitForIdle()
}

private fun MacrobenchmarkScope.ensureWorkdayAddActionVisible(): Boolean {
    repeat(8) {
        val hasAddActionText = WORKDAY_ADD_ACTION_TEXTS.any { label -> device.hasObject(By.text(label)) }
        if (hasAddActionText) {
            return true
        }
        scrollTowardWorkdayActionButtons()
    }
    return WORKDAY_ADD_ACTION_TEXTS.any { label -> device.hasObject(By.text(label)) }
}

private fun MacrobenchmarkScope.buildWorkdayNavigationDebugSnapshot(): String {
    val visibleTexts = device.findObjects(By.clazz("android.widget.TextView"))
        .asSequence()
        .mapNotNull { node -> runCatching { node.text?.trim() }.getOrNull() }
        .filter { it.isNotEmpty() }
        .distinct()
        .take(MAX_NAV_DEBUG_TOKENS)
        .joinToString(" | ")

    val buttonHints = device.findObjects(By.clazz("android.widget.Button"))
        .asSequence()
        .mapNotNull { node ->
            runCatching {
                val text = node.text?.trim().orEmpty()
                val desc = node.contentDescription?.toString()?.trim().orEmpty()
                val label = listOf(text, desc).firstOrNull { it.isNotBlank() } ?: "<no-label>"
                "$label@${node.visibleBounds.centerX()},${node.visibleBounds.centerY()}"
            }.getOrNull()
        }
        .distinct()
        .take(MAX_NAV_DEBUG_TOKENS)
        .joinToString(" | ")

    return "texts=[$visibleTexts] buttons=[$buttonHints]"
}

private fun MacrobenchmarkScope.attemptOpenSingleProjectViaEditAction(): Boolean {
    val candidateProject = device.findObjects(By.clazz("android.widget.TextView"))
        .asSequence()
        .mapNotNull { node ->
            runCatching {
                val text = node.text?.trim().orEmpty()
                if (text.isBlank()) return@runCatching null

                val excluded = WORKDAY_READY_TEXTS + WORKDAY_ADD_ACTION_TEXTS + WORKDAY_EDIT_ACTION_TEXTS +
                    listOf("Delete", "Poista", "Radera", "Nullify", "Nollaa")
                if (excluded.contains(text)) return@runCatching null

                val y = node.visibleBounds.centerY()
                val inContentBand = y in (device.displayHeight * 0.24f).toInt()..(device.displayHeight * 0.78f).toInt()
                if (!inContentBand) return@runCatching null

                node
            }.getOrNull()
        }
        .firstOrNull()

    if (candidateProject != null && clickNodeViaClickableAncestor(candidateProject)) {
        device.waitForIdle()
    }

    val editNode = WORKDAY_EDIT_ACTION_TEXTS
        .firstNotNullOfOrNull { text -> device.wait(Until.findObject(By.text(text)), CLICK_RETRY_WAIT_MS) }

    if (editNode != null && clickNodeViaClickableAncestor(editNode)) {
        device.waitForIdle()
        if (isLikelySingleProjectScreenVisible()) {
            return true
        }
        closeTransientDialogIfPresent()
    }

    return false
}

private fun MacrobenchmarkScope.clickNodeViaClickableAncestor(node: UiObject2): Boolean {
    var current: UiObject2? = node
    repeat(8) {
        val candidate = current ?: return@repeat
        if (candidate.isClickable) {
            candidate.click()
            return true
        }
        current = candidate.parent
    }

    // Last-resort tap on original node bounds when hierarchy metadata is ambiguous.
    val bounds = node.visibleBounds
    return device.click(bounds.centerX(), bounds.centerY())
}



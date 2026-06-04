@file:Suppress("TooManyFunctions")

package com.akiwiksten.awtimesheet.macrobenchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

// ---------------------------------------------------------------------------
// Shared timing constants
// ---------------------------------------------------------------------------
internal const val NAVIGATION_WAIT_MS = 2_000L
internal const val CALENDAR_READY_WAIT_MS = 4_000L
internal const val INTRO_DISMISS_TAPS = 6
internal const val INTRO_DISMISS_WAIT_MS = 1_500L

// How long to wait for bottom-nav tabs to appear after the intro is dismissed.
// Must cover the Compose navigation transition (typically < 500 ms) plus
// the intro animation duration (3 000 ms) in the worst case where the first
// tap fires before the animation finishes.
internal const val POST_INTRO_TABS_WAIT_MS = 4_000L

// ---------------------------------------------------------------------------
// Shared layout constants
// ---------------------------------------------------------------------------
internal const val BOTTOM_NAV_MIN_Y_RATIO = 0.82f

// ---------------------------------------------------------------------------
// Shared navigation labels
// ---------------------------------------------------------------------------
internal const val TAB_CALENDAR = "Calendar"
internal const val TAB_WORKDAY = "Workday"
internal const val TAB_SETTINGS = "Settings"

// ---------------------------------------------------------------------------
// Benchmark data seeding
// ---------------------------------------------------------------------------
internal const val BENCHMARK_SEED_ACTION =
    "com.akiwiksten.awtimesheet.benchmark.ACTION_SEED_REALISTIC_STARTUP_DATA"
internal const val BENCHMARK_SEED_RECEIVER_CLASS =
    "com.akiwiksten.awtimesheet.benchmark.BenchmarkDataSeedReceiver"
internal const val BENCHMARK_SEED_DATASET_EXISTING = "existing"
internal const val BENCHMARK_SEED_DATASET_EMPTY = "empty"

// ---------------------------------------------------------------------------
// Ready-state text signals
// ---------------------------------------------------------------------------

// Workday-ready markers across supported locales (en / fi / sv).
// We include both action labels and screen-identifying text so the benchmark
// remains stable even when the Add button is temporarily off-screen/unavailable.
internal val WORKDAY_READY_TEXTS = listOf(
    "Add", "Lisää", "Lägg till",
    "Workday", "Työpäivä", "Arbetsdag",
    "No projects available", "Ei projekteja saatavilla", "Inga projekt tillgängliga"
)

// SingleProjectScreen action labels across supported locales (en / fi / sv).
internal val SINGLE_PROJECT_READY_TEXTS = listOf(
    "Details",
    "Tiedot",
    "Detaljer",
    "Pick",
    "Valitse",
    "Välj"
)

// ---------------------------------------------------------------------------
// Scroll constants
// ---------------------------------------------------------------------------
private const val SWIPE_STEPS = 24
private const val STRESS_SWIPE_REPEATS = 6
private const val INTERACTION_SWIPE_REPEATS = 3
private const val NAV_FALLBACK_RETRIES = 5

// ---------------------------------------------------------------------------
// Navigation helpers
// ---------------------------------------------------------------------------

/**
 * Navigates to a bottom-nav tab by text label, with intro-dismiss and
 * coordinate-based fallback.
 */
internal fun MacrobenchmarkScope.openBottomNavTab(label: String) {
    device.waitForIdle()

    val clickedWithoutIntroDismiss = clickBottomNavTabByText(
        label = label,
        timeoutMs = NAVIGATION_WAIT_MS
    )

    // Intro-first builds can start on a full-screen intro screen.
    // Dismiss it and wait long enough for the 3 s intro animation to
    // finish and the Compose navigation transition to complete.
    if (!clickedWithoutIntroDismiss) {
        dismissIntroIfPresent()
    }

    val clickedByText = clickedWithoutIntroDismiss || clickBottomNavTabByText(
        label = label,
        timeoutMs = POST_INTRO_TABS_WAIT_MS
    )
    val clickedByFallback = !clickedByText && clickBottomNavTabByIndex(label)
    val clicked = clickedByText || clickedByFallback

    check(clicked) { "Could not find bottom nav tab '$label'." }
}

private fun MacrobenchmarkScope.clickBottomNavTabByText(
    label: String,
    timeoutMs: Long
): Boolean {
    val textTab = device.wait(Until.findObject(By.text(label)), timeoutMs)
    val clicked = textTab != null
    if (clicked) {
        textTab.click()
        device.waitForIdle()
    }
    return clicked
}

private fun MacrobenchmarkScope.clickBottomNavTabByIndex(label: String): Boolean {
    // Fallback: select bottom-nav item by its stable positional order.
    val targetIndex = when (label) {
        TAB_CALENDAR -> 0
        TAB_WORKDAY -> 1
        TAB_SETTINGS -> 2
        else -> null
    }

    var clicked = false
    if (targetIndex != null) {
        repeat(NAV_FALLBACK_RETRIES) {
            if (!clicked) {
                val candidates = device.findObjects(By.clickable(true))
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

                if (candidates.size >= 3) {
                    val (x, y) = candidates[targetIndex]
                    clicked = device.click(x, y)
                    if (clicked) {
                        device.waitForIdle()
                    }
                }
            }
            device.waitForIdle()
        }
    }
    return clicked
}

/**
 * Taps the centre of the screen up to [INTRO_DISMISS_TAPS] times until any
 * bottom-nav tab label becomes visible. Returns true if tabs appeared.
 *
 * The intro animation runs for ~3 s.  Taps fired before the animation
 * finishes still register on the clickable intro content and trigger
 * navigation, but the tab labels only appear after the Compose transition
 * completes.  This function keeps tapping and checking with generous delays
 * so the caller does not need to know the animation length.
 */
internal fun MacrobenchmarkScope.dismissIntroIfPresent(): Boolean {
    val tabsWereAlreadyVisible =
        device.hasObject(By.text(TAB_CALENDAR)) ||
            device.hasObject(By.text(TAB_WORKDAY)) ||
            device.hasObject(By.text(TAB_SETTINGS))

    val centerX = device.displayWidth / 2
    val centerY = device.displayHeight / 2

    var tabsBecameVisible = false

    if (!tabsWereAlreadyVisible) {
        repeat(INTRO_DISMISS_TAPS) {
            if (!tabsBecameVisible) {
                device.click(centerX, centerY)
                device.waitForIdle()

                tabsBecameVisible =
                    device.wait(Until.hasObject(By.text(TAB_WORKDAY)), INTRO_DISMISS_WAIT_MS) ||
                    device.hasObject(By.text(TAB_CALENDAR)) ||
                    device.hasObject(By.text(TAB_SETTINGS))
            }
        }
    }

    return tabsBecameVisible
}

// ---------------------------------------------------------------------------
// Ready-state helpers
// ---------------------------------------------------------------------------

/** Waits for the Calendar screen's EditText indicator (up to [CALENDAR_READY_WAIT_MS]). */
internal fun MacrobenchmarkScope.waitForCalendarScreenReady() {
    val ready = device.wait(Until.hasObject(By.clazz("android.widget.EditText")), CALENDAR_READY_WAIT_MS)
    check(ready) { "Calendar screen did not reach ready state." }
    device.waitForIdle()
}

/** Waits for Workday screen markers (locale-aware). */
internal fun MacrobenchmarkScope.waitForWorkdayScreenReady() {
    val ready = WORKDAY_READY_TEXTS.any { text ->
        device.wait(Until.hasObject(By.text(text)), NAVIGATION_WAIT_MS / WORKDAY_READY_TEXTS.size)
    }
    check(ready) {
        "Workday screen did not reach a ready state (missing known localized markers)."
    }
    device.waitForIdle()
}

/** Waits for the Settings screen's "Save" action button (falls back to label text). */
internal fun MacrobenchmarkScope.waitForSettingsScreenReady() {
    val ready = device.wait(Until.hasObject(By.text("Save")), NAVIGATION_WAIT_MS) ||
        device.wait(Until.hasObject(By.text("Settings")), NAVIGATION_WAIT_MS)
    check(ready) { "Settings screen did not reach a ready state." }
    device.waitForIdle()
}

/** Waits for SingleProjectScreen's editable text fields to appear. */
internal fun MacrobenchmarkScope.waitForSingleProjectScreenReady() {
    val deadlineMs = System.currentTimeMillis() + NAVIGATION_WAIT_MS
    var latestCount = 0
    var latestHasActionLabel = false
    while (System.currentTimeMillis() <= deadlineMs) {
        latestCount = device.findObjects(By.clazz("android.widget.EditText")).size
        latestHasActionLabel = SINGLE_PROJECT_READY_TEXTS.any { label ->
            device.hasObject(By.text(label))
        }
        if (latestCount >= 1 && latestHasActionLabel) {
            device.waitForIdle()
            return
        }
        device.waitForIdle()
    }
    check(false) {
        "SingleProject screen did not reach ready state (editTexts=$latestCount, hasActionLabel=$latestHasActionLabel)."
    }
}

/** Verifies target app stays foregrounded after launch/navigation steps. */
internal fun MacrobenchmarkScope.ensureTargetAppForegroundVisible() {
    val hasTargetPackage =
        device.wait(Until.hasObject(By.pkg(BenchmarkConfig.TARGET_PACKAGE)), NAVIGATION_WAIT_MS)
    check(hasTargetPackage) {
        "Target package '${BenchmarkConfig.TARGET_PACKAGE}' is not visible after launch."
    }
    device.waitForIdle()
}

// ---------------------------------------------------------------------------
// Scroll helper
// ---------------------------------------------------------------------------

private fun MacrobenchmarkScope.performVerticalScroll(repeats: Int) {
    val centerX = device.displayWidth / 2
    val topY = (device.displayHeight * 0.20f).toInt()
    val bottomY = (device.displayHeight * 0.80f).toInt()

    repeat(repeats) {
        device.swipe(centerX, bottomY, centerX, topY, SWIPE_STEPS)
        device.waitForIdle()
    }
    repeat(repeats) {
        device.swipe(centerX, topY, centerX, bottomY, SWIPE_STEPS)
        device.waitForIdle()
    }
}

/** Performs repeated full-height up/down swipes to stress scroll performance. */
internal fun MacrobenchmarkScope.performVerticalStressScroll() {
    performVerticalScroll(repeats = STRESS_SWIPE_REPEATS)
}

/**
 * Performs a lighter swipe sequence for recomposition tests.
 * This still produces deterministic UI updates but lowers thermal/CPU pressure.
 */
internal fun MacrobenchmarkScope.performVerticalInteractionScroll() {
    performVerticalScroll(repeats = INTERACTION_SWIPE_REPEATS)
}

/**
 * Seeds a realistic startup dataset through the benchmark-only receiver.
 *
 * This runs in setup, outside measured startup timing windows.
 */
internal fun MacrobenchmarkScope.seedRealisticStartupDataIfEmpty() {
    val receiverComponent = "${BenchmarkConfig.TARGET_PACKAGE}/$BENCHMARK_SEED_RECEIVER_CLASS"
    val output = device.executeShellCommand(
        "am broadcast -a $BENCHMARK_SEED_ACTION -n $receiverComponent"
    )
    val completed = output.contains("Broadcast completed")
    val hasError = output.contains("Error:") || output.contains("Exception")
    check(completed && !hasError) {
        "Benchmark data seeding broadcast failed. Output: $output"
    }
}

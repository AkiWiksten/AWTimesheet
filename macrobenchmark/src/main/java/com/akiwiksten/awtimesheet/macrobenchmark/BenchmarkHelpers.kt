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


// ---------------------------------------------------------------------------
// Scroll constants (shared by both ScrollBenchmark and RecompositionBenchmark)
// ---------------------------------------------------------------------------
private const val SWIPE_STEPS = 24
private const val SWIPE_REPEATS = 6
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

    // Primary path: text label lookup.
    val textTab = device.wait(Until.findObject(By.text(label)), NAVIGATION_WAIT_MS)
    if (textTab != null) {
        textTab.click()
        device.waitForIdle()
        return
    }

    // Intro-first builds can start on a full-screen intro screen.
    // Dismiss it and wait long enough for the 3 s intro animation to
    // finish and the Compose navigation transition to complete.
    dismissIntroIfPresent()
    val textTabAfterIntro = device.wait(Until.findObject(By.text(label)), POST_INTRO_TABS_WAIT_MS)
    if (textTabAfterIntro != null) {
        textTabAfterIntro.click()
        device.waitForIdle()
        return
    }

    // Fallback: select bottom-nav item by its stable positional order.
    val targetIndex = when (label) {
        TAB_CALENDAR -> 0
        TAB_WORKDAY  -> 1
        TAB_SETTINGS -> 2
        else         -> null
    }

    if (targetIndex != null) {
        repeat(NAV_FALLBACK_RETRIES) {
            val candidates = device.findObjects(By.clickable(true))
                .asSequence()
                .mapNotNull { node ->
                    runCatching {
                        val bounds = node.visibleBounds
                        val centerY = bounds.centerY()
                        if (centerY >= (device.displayHeight * BOTTOM_NAV_MIN_Y_RATIO).toInt()) {
                            bounds.centerX() to centerY
                        } else null
                    }.getOrNull()
                }
                .sortedBy { it.first }
                .toList()

            if (candidates.size >= 3) {
                val (x, y) = candidates[targetIndex]
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
    if (device.hasObject(By.text(TAB_CALENDAR)) ||
        device.hasObject(By.text(TAB_WORKDAY)) ||
        device.hasObject(By.text(TAB_SETTINGS))
    ) return false

    val centerX = device.displayWidth / 2
    val centerY = device.displayHeight / 2

    repeat(INTRO_DISMISS_TAPS) {
        device.click(centerX, centerY)
        device.waitForIdle()

        val tabsVisible =
            device.wait(Until.hasObject(By.text(TAB_WORKDAY)), INTRO_DISMISS_WAIT_MS) ||
            device.hasObject(By.text(TAB_CALENDAR)) ||
            device.hasObject(By.text(TAB_SETTINGS))
        if (tabsVisible) return true
    }
    return false
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
    val ready = device.wait(Until.hasObject(By.clazz("android.widget.EditText")), NAVIGATION_WAIT_MS)
    check(ready) { "SingleProject screen did not reach ready state (no editable fields visible)." }
    device.waitForIdle()
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

/** Performs repeated full-height up/down swipes to stress scroll performance. */
internal fun MacrobenchmarkScope.performVerticalStressScroll() {
    val centerX = device.displayWidth / 2
    val topY    = (device.displayHeight * 0.20f).toInt()
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

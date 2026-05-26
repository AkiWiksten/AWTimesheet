package com.akiwiksten.awtimesheet.macrobenchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until

// ---------------------------------------------------------------------------
// Shared timing constants
// ---------------------------------------------------------------------------
internal const val NAVIGATION_WAIT_MS = 2_000L
internal const val CALENDAR_READY_WAIT_MS = 4_000L
internal const val INTRO_DISMISS_TAPS = 3
internal const val INTRO_DISMISS_WAIT_MS = 1_200L

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
internal const val WORKDAY_READY_TEXT = "Add"

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
    // Dismiss it and retry text lookup before falling back to coordinates.
    dismissIntroIfPresent()
    val textTabAfterIntro = device.wait(Until.findObject(By.text(label)), NAVIGATION_WAIT_MS)
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

/** Waits for the Workday screen's "Add" button. */
internal fun MacrobenchmarkScope.waitForWorkdayScreenReady() {
    val ready = device.wait(Until.hasObject(By.text(WORKDAY_READY_TEXT)), NAVIGATION_WAIT_MS)
    check(ready) { "Workday screen did not reach a ready state (missing '$WORKDAY_READY_TEXT' text)." }
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


package com.akiwiksten.awtimesheet.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsBackNavigationGuardTest {

    @Test
    fun guardedBack_onSettingsWithUnsavedChanges_blocksNavigation() {
        val backStack = mutableStateListOf<Any>(Screen.Calendar, Screen.Settings)
        var blocked = false

        val onBack = createGuardedBackAction(
            backStack = backStack,
            hasUnsavedChanges = true,
            onUnsavedChangesBlocked = { blocked = true }
        )

        onBack()

        assertTrue(blocked)
        assertEquals(listOf(Screen.Calendar, Screen.Settings), backStack)
    }

    @Test
    fun guardedBack_onSettingsWithoutUnsavedChanges_popsBackStack() {
        val backStack = mutableStateListOf<Any>(Screen.Calendar, Screen.Settings)
        var blocked = false

        val onBack = createGuardedBackAction(
            backStack = backStack,
            hasUnsavedChanges = false,
            onUnsavedChangesBlocked = { blocked = true }
        )

        onBack()

        assertFalse(blocked)
        assertEquals(listOf(Screen.Calendar), backStack)
    }

    @Test
    fun guardedBack_notOnSettings_popsBackStackEvenWhenUnsavedFlagTrue() {
        val backStack = mutableStateListOf<Any>(Screen.Calendar, Screen.Workday)
        var blocked = false

        val onBack = createGuardedBackAction(
            backStack = backStack,
            hasUnsavedChanges = true,
            onUnsavedChangesBlocked = { blocked = true }
        )

        onBack()

        assertFalse(blocked)
        assertEquals(listOf(Screen.Calendar), backStack)
    }
}


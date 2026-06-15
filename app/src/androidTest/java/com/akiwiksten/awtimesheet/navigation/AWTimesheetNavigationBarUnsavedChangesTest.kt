package com.akiwiksten.awtimesheet.navigation

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akiwiksten.awtimesheet.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.akiwiksten.awtimesheet.core.R as CoreR

@RunWith(AndroidJUnit4::class)
class AWTimesheetNavigationBarUnsavedChangesTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun leavingSettings_withUnsavedChanges_showsConfirmationDialog() {
        val backStack = mutableStateListOf<Any>(Screen.Settings)

        setContentForTest(backStack = backStack)

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.calendar)).performClick()

        composeRule.onNodeWithText(composeRule.activity.getString(CoreR.string.unsaved_data_message))
            .assertIsDisplayed()
        assertEquals(Screen.Settings, backStack.last())
    }

    @Test
    fun leavingSettings_save_confirmsAndNavigates() {
        val backStack = mutableStateListOf<Any>(Screen.Settings)
        var saveCount = 0
        var discardCount = 0

        setContentForTest(
            backStack = backStack,
            onSaveSettingsChanges = { saveCount++ },
            onDiscardSettingsChanges = { discardCount++ }
        )

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.calendar)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(com.akiwiksten.awtimesheet.core.R.string.save))
            .performClick()

        assertEquals(1, saveCount)
        assertEquals(0, discardCount)
        assertEquals(Screen.Calendar, backStack.last())
    }

    @Test
    fun leavingSettings_discard_confirmsAndNavigates() {
        val backStack = mutableStateListOf<Any>(Screen.Settings)
        var saveCount = 0
        var discardCount = 0

        setContentForTest(
            backStack = backStack,
            onSaveSettingsChanges = { saveCount++ },
            onDiscardSettingsChanges = { discardCount++ }
        )

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.workday)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(com.akiwiksten.awtimesheet.core.R.string.discard))
            .performClick()

        assertEquals(0, saveCount)
        assertEquals(1, discardCount)
        assertEquals(Screen.Workday, backStack.last())
    }

    @Test
    fun leavingSettings_stay_keepsCurrentScreen() {
        val backStack = mutableStateListOf<Any>(Screen.Settings)
        var saveCount = 0
        var discardCount = 0

        setContentForTest(
            backStack = backStack,
            onSaveSettingsChanges = { saveCount++ },
            onDiscardSettingsChanges = { discardCount++ }
        )

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.calendar)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(com.akiwiksten.awtimesheet.core.R.string.stay))
            .performClick()

        assertEquals(0, saveCount)
        assertEquals(0, discardCount)
        assertEquals(Screen.Settings, backStack.last())
        composeRule.onAllNodesWithText(composeRule.activity.getString(CoreR.string.unsaved_data_message))
            .assertCountEquals(0)
    }

    private fun setContentForTest(
        backStack: SnapshotStateList<Any>,
        onSaveSettingsChanges: () -> Unit = {},
        onDiscardSettingsChanges: () -> Unit = {}
    ) {
        composeRule.setContent {
            MaterialTheme {
                AWTimesheetNavigationBar(
                    backStack = backStack,
                    settingsHasUnsavedChanges = true,
                    onSaveSettingsChanges = onSaveSettingsChanges,
                    onDiscardSettingsChanges = onDiscardSettingsChanges
                )
            }
        }
    }
}


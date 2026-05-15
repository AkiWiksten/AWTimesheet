package com.akiwiksten.awtimesheet.feature.settings

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsWorkTypeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsContent_showsOtherAsDefaultWorkType() {
        val otherText = composeRule.activity.getString(R.string.other)

        composeRule.setContent {
            MaterialTheme {
                SettingsContent(
                    uiState = settingsUiState(workTypes = listOf(otherText, "Support")),
                    actions = settingsActions(),
                    defaultWorkType = otherText
                )
            }
        }

        composeRule.onNodeWithText(otherText).assertIsDisplayed()
    }

    @Test
    fun settingsContent_changesWorkTypeOnlyAfterManualSelection() {
        val otherText = composeRule.activity.getString(R.string.other)
        val supportText = "Support"

        composeRule.setContent {
            MaterialTheme {
                SettingsContent(
                    uiState = settingsUiState(workTypes = listOf(otherText, supportText)),
                    actions = settingsActions(),
                    defaultWorkType = otherText
                )
            }
        }

        composeRule.onNodeWithText(otherText).assertIsDisplayed()
        composeRule.onAllNodesWithText(supportText).assertCountEquals(0)

        composeRule.onNodeWithText(otherText).performTouchInput { click() }
        composeRule.onNodeWithText(supportText).performClick()

        composeRule.onNodeWithText(supportText).assertIsDisplayed()
    }

    @Test
    fun settingsContent_addingWorkType_selectsAddedWorkType() {
        val otherText = composeRule.activity.getString(R.string.other)
        val workTypeLabel = composeRule.activity.getString(R.string.work_type)
        val addedWorkType = "Installation"

        composeRule.setContent {
            var uiState by rememberSettingsUiStateForTest(initialWorkTypes = listOf(otherText, "Support"))

            MaterialTheme {
                SettingsContent(
                    uiState = uiState,
                    actions = settingsActions(
                        onWorkTypeAdded = { addedType ->
                            uiState = settingsUiState(uiState.data.workTypes + addedType)
                        }
                    ),
                    defaultWorkType = otherText
                )
            }
        }

        composeRule.onNodeWithText(otherText).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.add)).performClick()
        composeRule.onNode(hasText(workTypeLabel) and hasSetTextAction()).performTextInput(addedWorkType)
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.confirm)).performClick()

        composeRule.onNodeWithText(addedWorkType).assertIsDisplayed()
    }

    private fun settingsUiState(workTypes: List<String>): SettingsUiState.Success {
        return SettingsUiState.Success(
            data = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "00:00",
                workTypes = workTypes
            ),
            selectedDate = "2026-05-05"
        )
    }

    private fun settingsActions(): SettingsActions {
        return settingsActions(onWorkTypeAdded = {})
    }

    private fun settingsActions(onWorkTypeAdded: (String) -> Unit): SettingsActions {
        return SettingsActions(
            onNameChange = {},
            onEmployerChange = {},
            onDailyWorkTimeEstimateChange = {},
            onDailyLunchTimeEstimateChange = {},
            onInitialFlexTimeTotalChange = {},
            onWorkTypeAdded = onWorkTypeAdded,
            onWorkTypeRemoved = {},
            onSave = {},
            onGeneratePdf = {}
        )
    }

    @Composable
    private fun rememberSettingsUiStateForTest(initialWorkTypes: List<String>) =
        remember { mutableStateOf(settingsUiState(initialWorkTypes)) }
}


package com.akiwiksten.awtimesheet.feature.singleproject

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectConfiguration
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectScreenParams
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectScreenState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SingleProjectDetailsButtonStateTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun detailsButton_isDisabled_whenProjectNameIsEmpty() {
        val detailsText = composeRule.activity.getString(R.string.details)

        composeRule.setContent {
            MaterialTheme {
                SingleProjectScreenContent(
                    params = SingleProjectScreenParams(
                        screenState = screenState(projectName = ""),
                        actions = testActions(),
                        config = SingleProjectConfiguration(
                            absencePrefix = "Absence",
                            flexDayWorkType = "Absence-Flex day"
                        )
                    ),
                    hasUnsavedChanges = false,
                    onNavigateBack = {}
                )
            }
        }

        composeRule.onNodeWithText(detailsText).assertIsNotEnabled()
    }

    @Test
    fun detailsButton_isEnabled_whenProjectNameIsNotEmpty() {
        val detailsText = composeRule.activity.getString(R.string.details)

        composeRule.setContent {
            MaterialTheme {
                SingleProjectScreenContent(
                    params = SingleProjectScreenParams(
                        screenState = screenState(projectName = "Project A"),
                        actions = testActions(),
                        config = SingleProjectConfiguration(
                            absencePrefix = "Absence",
                            flexDayWorkType = "Absence-Flex day"
                        )
                    ),
                    hasUnsavedChanges = false,
                    onNavigateBack = {}
                )
            }
        }

        composeRule.onNodeWithText(detailsText).assertIsEnabled()
    }

    private fun screenState(projectName: String): SingleProjectScreenState {
        val state = SingleProjectState(
            projectName = projectName,
            projectTime = "01:00",
            allowance = "No allowance",
            workType = "Other",
            date = "2026-05-05"
        )

        return SingleProjectScreenState(
            date = state.date,
            editedProjectIndex = state.listIndex,
            state = state,
            isAddMode = state.isAddMode,
            uiState = SingleProjectUiState.Success(
                data = state,
                workTimeByDate = "01:00",
                workTypes = listOf("Other")
            ),
            isConfirmEnabled = false,
            isDuplicateProjectName = false,
            isTimePickerDisabled = false
        )
    }

    private fun testActions() = SingleProjectActions(
        onStateChange = {},
        onOpenProjectDetails = {},
        onSave = {}
    )
}


package com.akiwiksten.awtimesheet.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.akiwiksten.awtimesheet.core.theme.AWTimesheetTheme
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsActions
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsStateContentState
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true, name = "Settings - Loading")
@Composable
fun PreviewSettingsLoading() {
    SettingsPreviewContent(uiState = SettingsUiState.Loading)
}

@PreviewTest
@Preview(showBackground = true, name = "Settings - Success")
@Composable
fun PreviewSettingsSuccess() {
    SettingsPreviewContent(
        uiState = SettingsUiState.Success(
            data = SettingsState(
                name = "Aki Wiksten",
                employer = "WorkTime Oy",
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:00",
                workTypes = listOf("Installation", "Maintenance", "Meeting")
            ),
            selectedDate = "2026-04-19"
        )
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Settings - Error")
@Composable
fun PreviewSettingsError() {
    SettingsPreviewContent(uiState = SettingsUiState.Error(message = "Failed to load settings"))
}

@Composable
private fun SettingsPreviewContent(uiState: SettingsUiState) {
    AWTimesheetTheme(dynamicColor = false) {
        SettingsStateContent(
            state = SettingsStateContentState(
                uiState = uiState,
                defaultWorkTypes = listOf("Other"),
                onUnsavedChangesChanged = {},
                registerUnsavedActions = { _, _ -> },
                onDiscardChanges = {},
                createActions = {
                    SettingsActions(
                        onNameChange = {},
                        onEmployerChange = {},
                        onDailyWorkTimeEstimateChange = {},
                        onDailyLunchTimeEstimateChange = {},
                        onInitialFlexTimeTotalChange = {},
                        onWorkTypeAdded = {},
                        onWorkTypeRemoved = {},
                        onSave = {},
                        onGenerateXlsx = {},
                        onGenerateWorkdaysForMonth = {},
                        onGenerateWorkdaysForYear = {}
                    )
                }
            )
        )
    }
}

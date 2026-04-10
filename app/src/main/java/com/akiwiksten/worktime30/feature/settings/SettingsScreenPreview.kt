package com.akiwiksten.worktime30.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity

@Preview(showBackground = true, name = "Settings - Loading")
@Composable
fun PreviewSettingsLoading() {
    SettingsPreviewContent(uiState = SettingsUiState.Loading)
}

@Preview(showBackground = true, name = "Settings - Success")
@Composable
fun PreviewSettingsSuccess() {
    SettingsPreviewContent(
        uiState = SettingsUiState.Success(
            name = "Aki Wiksten",
            employer = "WorkTime Oy",
            endMonthDate = "2026-04-30",
            workTypes = listOf("Installation", "Maintenance", "Meeting"),
            projectsByMonth = listOf(
                ProjectEntity(
                    date = "2026-04-10",
                    projectName = "Alpha Site",
                    projectTime = "04:00",
                    kilometres = 20,
                    allowance = "Daily allowance",
                    workType = "Installation"
                )
            )
        )
    )
}

@Preview(showBackground = true, name = "Settings - Error")
@Composable
fun PreviewSettingsError() {
    SettingsPreviewContent(uiState = SettingsUiState.Error(message = "Failed to load settings"))
}

@Composable
private fun SettingsPreviewContent(uiState: SettingsUiState) {
    WorkTime30Theme(dynamicColor = false) {
        SettingsStateContent(
            uiState = uiState,
            calendarDate = "2026-04-10",
            createActions = {
                SettingsActions(
                    onNameChange = {},
                    onEmployerChange = {},
                    onWorkTypeAdded = {},
                    onWorkTypeRemoved = {},
                    onSave = {},
                    onGeneratePdf = {}
                )
            }
        )
    }
}

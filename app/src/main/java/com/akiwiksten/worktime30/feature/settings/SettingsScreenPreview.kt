package com.akiwiksten.worktime30.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
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
                selectedDate = "2026-04-19",
                endMonthDate = "2026-04-30",
                workTypes = listOf("Installation", "Maintenance", "Meeting"),
                projectsByMonth = listOf(
                    SingleProjectState(
                        date = "2026-04-10",
                        projectName = "Alpha Site",
                        projectTime = "04:00",
                        kilometres = 20.toString(),
                        allowance = "Daily allowance",
                        workType = "Installation"
                    )
                )
            )
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
    WorkTime30Theme(dynamicColor = false) {
        SettingsStateContent(
            uiState = uiState,
            createActions = {
                SettingsActions(
                    onNameChange = {},
                    onEmployerChange = {},
                    onDailyWorkTimeEstimateChange = {},
                    onDailyLunchTimeEstimateChange = {},
                    onWorkTypeAdded = {},
                    onWorkTypeRemoved = {},
                    onSave = {},
                    onGeneratePdf = {}
                )
            }
        )
    }
}

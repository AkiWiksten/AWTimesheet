package com.akiwiksten.awtimesheet.feature.workday

import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.akiwiksten.awtimesheet.core.theme.AWTimesheetTheme
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.android.tools.screenshot.PreviewTest

private const val PREVIEW_DATE = "2026-04-10"

@PreviewTest
@Preview(showBackground = true, name = "Workday - Loading")
@Composable
fun PreviewWorkdayLoading() {
    WorkdayPreviewContent(
        uiState = WorkdayUiState.Loading,
        selectedItemIndex = -1
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Workday - Success")
@Composable
fun PreviewWorkdaySuccess() {
    WorkdayPreviewContent(
        uiState = WorkdayUiState.Success(
            date = PREVIEW_DATE,
            workTimeByDate = "07:45",
            projects = listOf(
                SingleProjectState(
                    date = PREVIEW_DATE,
                    index = 0,
                    projectName = "Alpha Site",
                    projectTime = "04:15",
                    kilometres = "24",
                    allowance = "Daily allowance",
                    workType = "Installation",
                ),
                SingleProjectState(
                    date = PREVIEW_DATE,
                    index = 1,
                    projectName = "Beta Support",
                    projectTime = "03:30",
                    kilometres = "8",
                    allowance = "",
                    workType = "Maintenance",
                ),
                SingleProjectState(
                    index = 2,
                    projectName = "Gamma Planning",
                )
            )
        ),
        selectedItemIndex = 0
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Workday - Empty")
@Composable
fun PreviewWorkdayEmpty() {
    WorkdayPreviewContent(
        uiState = WorkdayUiState.Success(
            date = PREVIEW_DATE,
            workTimeByDate = "00:00",
            projects = emptyList()
        ),
        selectedItemIndex = -1
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Workday - Error")
@Composable
fun PreviewWorkdayError() {
    WorkdayPreviewContent(
        uiState = WorkdayUiState.Error(message = "Failed to load projects"),
        selectedItemIndex = -1
    )
}

@Composable
private fun WorkdayPreviewContent(
    uiState: WorkdayUiState,
    selectedItemIndex: Int
) {
    val scrollState = rememberScrollState()
    AWTimesheetTheme(dynamicColor = false) {
        WorkdayContent(
            workdayUiState = uiState,
            workTimeByDateChange = "00:00",
            selectedItemIndex = selectedItemIndex,
            scrollState = scrollState,
            actions = WorkdayActions(
                onSelectedItemIndexChange = {},
                onTrackProjectEditorLaunch = { _, _ -> },
                onNavigateToSingleProject = {},
                onRetry = {},
                onSaveSettings = { _, _ -> },
                onDeleteProject = {}
            )
        )
    }
}

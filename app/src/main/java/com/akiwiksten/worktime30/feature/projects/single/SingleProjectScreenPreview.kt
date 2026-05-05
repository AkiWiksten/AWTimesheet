package com.akiwiksten.worktime30.feature.projects.single

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.feature.workday.WorkdayUiState
import com.android.tools.screenshot.PreviewTest

private const val PREVIEW_DATE = "2026-04-10"

@PreviewTest
@Preview(showBackground = true, name = "Single Project - Loading")
@Composable
fun PreviewSingleProjectLoading() {
    SingleProjectPreviewContent(
        params = SingleProjectScreenContentParams(
            date = PREVIEW_DATE,
            state = SingleProjectState(),
            isAddMode = true,
            projectsUiState = WorkdayUiState.Loading,
            isConfirmEnabled = false,
            hasUnsavedChanges = false,
            onStateChange = {},
            onNavigateBack = {},
            onOpenProjectDetails = {},
            onConfirm = {}
        )
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Single Project - Success Add")
@Composable
fun PreviewSingleProjectSuccessAdd() {
    SingleProjectPreviewContent(
        params = SingleProjectScreenContentParams(
            date = PREVIEW_DATE,
            state = SingleProjectState(
                projectTime = "00:00",
                kilometres = "",
                allowance = "No allowance",
                workType = "Installation",
            ),
            isAddMode = true,
            projectsUiState = WorkdayUiState.Success(
                date = PREVIEW_DATE,
                workTimeByDate = "07:45",
                workTypes = listOf("Installation", "Maintenance", "Meeting")
            ),
            isConfirmEnabled = false,
            hasUnsavedChanges = false,
            onStateChange = {},
            onNavigateBack = {},
            onOpenProjectDetails = {},
            onConfirm = {}
        )
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Single Project - Success Edit")
@Composable
fun PreviewSingleProjectSuccessEdit() {
    SingleProjectPreviewContent(
        params = SingleProjectScreenContentParams(
            date = PREVIEW_DATE,
            state = SingleProjectState(
                index = 0,
                projectName = "Beta Support",
                projectTime = "03:30",
                kilometres = "18",
                allowance = "Full allowance",
                workType = "Maintenance",
            ),
            isAddMode = false,
            projectsUiState = WorkdayUiState.Success(
                date = PREVIEW_DATE,
                workTimeByDate = "07:45",
                workTypes = listOf("Installation", "Maintenance", "Meeting")
            ),
            isConfirmEnabled = true,
            hasUnsavedChanges = true,
            onStateChange = {},
            onNavigateBack = {},
            onOpenProjectDetails = {},
            onConfirm = {}
        )
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Single Project - Error")
@Composable
fun PreviewSingleProjectError() {
    SingleProjectPreviewContent(
        params = SingleProjectScreenContentParams(
            date = PREVIEW_DATE,
            state = SingleProjectState(),
            isAddMode = true,
            projectsUiState = WorkdayUiState.Error(message = "Failed to load project"),
            isConfirmEnabled = false,
            hasUnsavedChanges = false,
            onStateChange = {},
            onNavigateBack = {},
            onOpenProjectDetails = {},
            onConfirm = {}
        )
    )
}

@Composable
private fun SingleProjectPreviewContent(params: SingleProjectScreenContentParams) {
    WorkTime30Theme(dynamicColor = false) {
        SingleProjectScreenContent(params = params)
    }
}

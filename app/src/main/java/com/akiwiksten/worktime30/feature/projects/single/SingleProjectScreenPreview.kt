package com.akiwiksten.worktime30.feature.projects.single

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme
import com.akiwiksten.worktime30.feature.projects.daily.ProjectsUiState
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState

private const val PREVIEW_DATE = "2026-04-10"

@Preview(showBackground = true, name = "Single Project - Loading")
@Composable
fun PreviewSingleProjectLoading() {
    SingleProjectPreviewContent(
        params = SingleProjectScreenContentParams(
            date = PREVIEW_DATE,
            state = SingleProjectState(),
            isAddMode = true,
            projectsUiState = ProjectsUiState.Loading,
            isConfirmEnabled = false,
            onStateChange = {},
            onNavigateBack = {},
            onOpenProjectDetails = {},
            onConfirm = {}
        )
    )
}

@Preview(showBackground = true, name = "Single Project - Success Add")
@Composable
fun PreviewSingleProjectSuccessAdd() {
    SingleProjectPreviewContent(
        params = SingleProjectScreenContentParams(
            date = PREVIEW_DATE,
            state = SingleProjectState(
                index = -1,
                projectName = "",
                projectTime = "00:00",
                kilometres = "",
                allowance = "No allowance",
                workType = "Installation"
            ),
            isAddMode = true,
            projectsUiState = ProjectsUiState.Success(
                date = PREVIEW_DATE,
                workTimeToday = "07:45",
                workTypes = listOf("Installation", "Maintenance", "Meeting")
            ),
            isConfirmEnabled = false,
            onStateChange = {},
            onNavigateBack = {},
            onOpenProjectDetails = {},
            onConfirm = {}
        )
    )
}

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
                workType = "Maintenance"
            ),
            isAddMode = false,
            projectsUiState = ProjectsUiState.Success(
                date = PREVIEW_DATE,
                workTimeToday = "07:45",
                workTypes = listOf("Installation", "Maintenance", "Meeting")
            ),
            isConfirmEnabled = true,
            onStateChange = {},
            onNavigateBack = {},
            onOpenProjectDetails = {},
            onConfirm = {}
        )
    )
}

@Preview(showBackground = true, name = "Single Project - Error")
@Composable
fun PreviewSingleProjectError() {
    SingleProjectPreviewContent(
        params = SingleProjectScreenContentParams(
            date = PREVIEW_DATE,
            state = SingleProjectState(),
            isAddMode = true,
            projectsUiState = ProjectsUiState.Error(message = "Failed to load project"),
            isConfirmEnabled = false,
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

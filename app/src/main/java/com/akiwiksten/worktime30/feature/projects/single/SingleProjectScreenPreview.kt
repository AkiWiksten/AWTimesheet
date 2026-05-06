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
            state = SingleProjectState(),
            isAddMode = true,
            isConfirmEnabled = false,
            hasUnsavedChanges = false,
            isDuplicateProjectName = false,
            onStateChange = {},
            onNavigateBack = {},
            onOpenProjectDetails = {},
            onConfirm = {},
            index = 0,
            singleProjectUiState = SingleProjectUiState.Loading
        )
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Single Project - Success Add")
@Composable
fun PreviewSingleProjectSuccessAdd() {
    SingleProjectPreviewContent(
        params = SingleProjectScreenContentParams(
            state = SingleProjectState(
                projectTime = "00:00",
                kilometres = "",
                allowance = "No allowance",
                workType = "Installation",
            ),
            isAddMode = true,
            isConfirmEnabled = false,
            hasUnsavedChanges = false,
            isDuplicateProjectName = false,
            onStateChange = {},
            onNavigateBack = {},
            onOpenProjectDetails = {},
            onConfirm = {},
            index = 0,
            singleProjectUiState = SingleProjectUiState.Success(
                data = SingleProjectState(
                    date = PREVIEW_DATE,
                    projectTime = "00:00",
                    kilometres = "",
                    allowance = "No allowance",
                    workType = "Installation",
                ),
                workTimeByDate = "07:45",
            )
        )
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Single Project - Success Edit")
@Composable
fun PreviewSingleProjectSuccessEdit() {
    SingleProjectPreviewContent(
        params = SingleProjectScreenContentParams(
            state = SingleProjectState(
                index = 0,
                projectName = "Beta Support",
                projectTime = "03:30",
                kilometres = "18",
                allowance = "Full allowance",
                workType = "Maintenance",
            ),
            isAddMode = false,
            isConfirmEnabled = true,
            hasUnsavedChanges = true,
            isDuplicateProjectName = false,
            onStateChange = {},
            onNavigateBack = {},
            onOpenProjectDetails = {},
            onConfirm = {},
            index = -1,
            singleProjectUiState = SingleProjectUiState.Success(
                data = SingleProjectState(
                    date = PREVIEW_DATE,
                    projectTime = "00:00",
                    kilometres = "",
                    allowance = "No allowance",
                    workType = "Installation",
                ),
                workTimeByDate = "07:45",
            )
        )
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Single Project - Duplicate Name")
@Composable
fun PreviewSingleProjectDuplicateName() {
    SingleProjectPreviewContent(
        params = SingleProjectScreenContentParams(
            state = SingleProjectState(
                index = -1,
                projectName = "Alpha Site",
                projectTime = "01:00",
            ),
            isAddMode = true,
            isConfirmEnabled = false,
            hasUnsavedChanges = true,
            isDuplicateProjectName = true,
            onStateChange = {},
            onNavigateBack = {},
            onOpenProjectDetails = {},
            onConfirm = {},
            index = 0,
            singleProjectUiState = SingleProjectUiState.Success(
                data = SingleProjectState(
                    date = PREVIEW_DATE,
                    projectTime = "00:00",
                    kilometres = "",
                    allowance = "No allowance",
                    workType = "Installation",
                ),
                workTimeByDate = "07:45",
            )
        )
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Single Project - Error")
@Composable
fun PreviewSingleProjectError() {
    SingleProjectPreviewContent(
        params = SingleProjectScreenContentParams(
            state = SingleProjectState(),
            isAddMode = true,isConfirmEnabled = false,
            hasUnsavedChanges = false,
            isDuplicateProjectName = false,
            onStateChange = {},
            onNavigateBack = {},
            onOpenProjectDetails = {},
            onConfirm = {},
            singleProjectUiState = SingleProjectUiState.Error(message = "Failed to load project")
        )
    )
}

@Composable
private fun SingleProjectPreviewContent(params: SingleProjectScreenContentParams) {
    WorkTime30Theme(dynamicColor = false) {
        SingleProjectScreenContent(params = params)
    }
}

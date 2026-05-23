package com.akiwiksten.awtimesheet.feature.singleproject

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.akiwiksten.awtimesheet.core.theme.AWTimesheetTheme
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.android.tools.screenshot.PreviewTest

private const val PREVIEW_DATE = "2026-04-10"
private const val NO_ALLOWANCE = "No allowance"

@PreviewTest
@Preview(showBackground = true, name = "Single Project - Loading")
@Composable
fun PreviewSingleProjectLoading() {
    SingleProjectPreviewContent(
        screenState = SingleProjectScreenState(
            date = "",
            editedProjectIndex = 0,
            state = SingleProjectState(),
            isAddMode = true,
            uiState = SingleProjectUiState.Loading,
            isConfirmEnabled = false,
            isDuplicateProjectName = false
        ),
        hasUnsavedChanges = false
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Single Project - Success Add")
@Composable
fun PreviewSingleProjectSuccessAdd() {
    SingleProjectPreviewContent(
        screenState = SingleProjectScreenState(
            date = PREVIEW_DATE,
            editedProjectIndex = 0,
            state = SingleProjectState(
                projectTime = "00:00",
                kilometres = "",
                allowance = NO_ALLOWANCE,
                workType = "Installation",
            ),
            isAddMode = true,
            uiState = SingleProjectUiState.Success(
                data = SingleProjectState(
                    date = PREVIEW_DATE,
                    projectTime = "00:00",
                    kilometres = "",
                    allowance = NO_ALLOWANCE,
                    workType = "Installation",
                ),
                workTimeByDate = "07:45",
            ),
            isConfirmEnabled = false,
            isDuplicateProjectName = false
        ),
        hasUnsavedChanges = false
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Single Project - Success Edit")
@Composable
fun PreviewSingleProjectSuccessEdit() {
    SingleProjectPreviewContent(
        screenState = SingleProjectScreenState(
            date = PREVIEW_DATE,
            editedProjectIndex = -1,
            state = SingleProjectState(
                index = 0,
                projectName = "Beta Support",
                projectTime = "03:30",
                kilometres = "18",
                allowance = "Full allowance",
                workType = "Maintenance",
            ),
            isAddMode = false,
            uiState = SingleProjectUiState.Success(
                data = SingleProjectState(
                    date = PREVIEW_DATE,
                    projectTime = "00:00",
                    kilometres = "",
                    allowance = NO_ALLOWANCE,
                    workType = "Installation",
                ),
                workTimeByDate = "07:45",
            ),
            isConfirmEnabled = true,
            isDuplicateProjectName = false
        ),
        hasUnsavedChanges = true
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Single Project - Duplicate Name")
@Composable
fun PreviewSingleProjectDuplicateName() {
    SingleProjectPreviewContent(
        screenState = SingleProjectScreenState(
            date = PREVIEW_DATE,
            editedProjectIndex = 0,
            state = SingleProjectState(
                index = -1,
                projectName = "Alpha Site",
                projectTime = "01:00",
            ),
            isAddMode = true,
            uiState = SingleProjectUiState.Success(
                data = SingleProjectState(
                    date = PREVIEW_DATE,
                    projectTime = "00:00",
                    kilometres = "",
                    allowance = NO_ALLOWANCE,
                    workType = "Installation",
                ),
                workTimeByDate = "07:45",
            ),
            isConfirmEnabled = false,
            isDuplicateProjectName = true
        ),
        hasUnsavedChanges = true
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Single Project - Error")
@Composable
fun PreviewSingleProjectError() {
    SingleProjectPreviewContent(
        screenState = SingleProjectScreenState(
            date = "",
            editedProjectIndex = -1,
            state = SingleProjectState(),
            isAddMode = true,
            uiState = SingleProjectUiState.Error(message = "Failed to load project"),
            isConfirmEnabled = false,
            isDuplicateProjectName = false
        ),
        hasUnsavedChanges = false
    )
}

@Composable
private fun SingleProjectPreviewContent(
    screenState: SingleProjectScreenState,
    hasUnsavedChanges: Boolean
) {
    AWTimesheetTheme(dynamicColor = false) {
        SingleProjectScreenContent(
            screenState = screenState,
            actions = SingleProjectActions(
                onStateChange = {},
                onOpenProjectDetails = {},
                onConfirm = {}
            ),
            hasUnsavedChanges = hasUnsavedChanges,
            onNavigateBack = {}
        )
    }
}


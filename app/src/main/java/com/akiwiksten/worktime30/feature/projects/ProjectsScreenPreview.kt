package com.akiwiksten.worktime30.feature.projects

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme

@Preview(showBackground = true, name = "Projects - Loading")
@Composable
fun PreviewProjectsLoading() {
    ProjectsPreviewContent(
        uiState = ProjectsUiState.Loading,
        selectedItemIndex = -1
    )
}

@Preview(showBackground = true, name = "Projects - Success")
@Composable
fun PreviewProjectsSuccess() {
    ProjectsPreviewContent(
        uiState = ProjectsUiState.Success(
            date = "2026-04-10",
            workTimeToday = "07:45",
            projects = listOf(
                ProjectListItemUiState(
                    index = 0,
                    projectName = "Alpha Site",
                    projectTime = "04:15",
                    kilometres = 24,
                    allowance = "Daily allowance",
                    workType = "Installation"
                ),
                ProjectListItemUiState(
                    index = 1,
                    projectName = "Beta Support",
                    projectTime = "03:30",
                    kilometres = 8,
                    allowance = "",
                    workType = "Maintenance"
                )
            )
        ),
        selectedItemIndex = 0
    )
}

@Preview(showBackground = true, name = "Projects - Empty")
@Composable
fun PreviewProjectsEmpty() {
    ProjectsPreviewContent(
        uiState = ProjectsUiState.Success(
            date = "2026-04-10",
            workTimeToday = "00:00",
            projects = emptyList()
        ),
        selectedItemIndex = -1
    )
}

@Preview(showBackground = true, name = "Projects - Error")
@Composable
fun PreviewProjectsError() {
    ProjectsPreviewContent(
        uiState = ProjectsUiState.Error(message = "Failed to load projects"),
        selectedItemIndex = -1
    )
}

@Composable
private fun ProjectsPreviewContent(
    uiState: ProjectsUiState,
    selectedItemIndex: Int
) {
    WorkTime30Theme(dynamicColor = false) {
        ProjectsContent(
            projectsUiState = uiState,
            selectedItemIndex = selectedItemIndex,
            actions = ProjectsActions(
                onSelectedItemIndexChange = {},
                onNavigateToSingleProject = {},
                onRetry = {},
                onDeleteProject = {}
            )
        )
    }
}

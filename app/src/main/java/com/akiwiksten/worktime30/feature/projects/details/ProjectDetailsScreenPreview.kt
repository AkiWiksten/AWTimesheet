package com.akiwiksten.worktime30.feature.projects.details

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme

@Preview(showBackground = true, name = "ProjectDetails - Loading")
@Composable
fun previewProjectDetailsLoading() {
    projectDetailsPreviewContent(uiState = ProjectDetailsUiState.Loading)
}

@Preview(showBackground = true, name = "ProjectDetails - Success New Day")
@Composable
fun previewProjectDetailsSuccessNewDay() {
    projectDetailsPreviewContent(
        uiState = ProjectDetailsUiState.Success(
            data = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha Site",
                workStats = WorkStatsState(
                    dailyWorkTimeEstimate = "07:30",
                    dailyLunchTimeEstimate = "00:30",
                    initialFlexTimeTotal = "+03:10"
                )
            )
        ),
        projectName = "Alpha Site"
    )
}

@Preview(showBackground = true, name = "ProjectDetails - Success Existing Day")
@Composable
fun previewProjectDetailsSuccessExistingDay() {
    projectDetailsPreviewContent(
        uiState = ProjectDetailsUiState.Success(
            data = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Beta Support",
                startTime = "08:00",
                endTime = "16:30",
                lunchStart = "11:30",
                lunchEnd = "12:00",
                breakStart = "14:15",
                breakEnd = "14:30",
                projectTime = "08:00",
                flexTimeToday = "+00:30",
                workStats = WorkStatsState(
                    dailyWorkTimeEstimate = "07:30",
                    dailyLunchTimeEstimate = "00:30",
                    initialFlexTimeTotal = "+04:10"
                )
            )
        ),
        projectName = "Beta Support"
    )
}

@Preview(showBackground = true, name = "ProjectDetails - Error")
@Composable
fun previewProjectDetailsError() {
    projectDetailsPreviewContent(
        uiState = ProjectDetailsUiState.Error(message = "Failed to load project details")
    )
}

@Composable
private fun projectDetailsPreviewContent(
    uiState: ProjectDetailsUiState,
    projectName: String? = null
) {
    WorkTime30Theme(dynamicColor = false) {
        ProjectDetailsStateContent(
            padding = PaddingValues(0.dp),
            uiState = uiState,
            projectName = projectName,
            actions = ProjectDetailsScreenActions(),
            isConfirmEnabled = uiState is ProjectDetailsUiState.Success
        )
    }
}

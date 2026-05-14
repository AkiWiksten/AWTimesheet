package com.akiwiksten.worktime30.feature.projects.details

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.android.tools.screenshot.PreviewTest

@PreviewTest
@Preview(showBackground = true, name = "ProjectDetails - Loading")
@Composable
fun PreviewProjectDetailsLoading() {
    ProjectDetailsPreviewContent(uiState = ProjectDetailsUiState.Loading)
}

@PreviewTest
@Preview(showBackground = true, name = "ProjectDetails - Success New Day")
@Composable
fun PreviewProjectDetailsSuccessNewDay() {
    ProjectDetailsPreviewContent(
        uiState = ProjectDetailsUiState.Success(
            details = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha Site"
            ),
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+03:10"
            )
        )
    )
}

@PreviewTest
@Preview(showBackground = true, name = "ProjectDetails - Success Existing Day")
@Composable
fun PreviewProjectDetailsSuccessExistingDay() {
    ProjectDetailsPreviewContent(
        uiState = ProjectDetailsUiState.Success(
            details = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Beta Support",
                startTime = "08:00",
                endTime = "16:30",
                lunchStart = "11:30",
                lunchEnd = "12:00",
                breakStart = "14:15",
                breakEnd = "14:30",
                projectTime = "08:00"
            ),
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+04:10"
            )
        )
    )
}

@PreviewTest
@Preview(showBackground = true, name = "ProjectDetails - Error")
@Composable
fun PreviewProjectDetailsError() {
    ProjectDetailsPreviewContent(
        uiState = ProjectDetailsUiState.Error(message = "Failed to load project details")
    )
}

@Composable
private fun ProjectDetailsPreviewContent(
    uiState: ProjectDetailsUiState
) {
    WorkTime30Theme(dynamicColor = false) {
        ProjectDetailsStateContent(
            padding = PaddingValues(0.dp),
            uiState = uiState,
            actions = ProjectDetailsScreenActions(),
            isConfirmEnabled = uiState is ProjectDetailsUiState.Success
        )
    }
}

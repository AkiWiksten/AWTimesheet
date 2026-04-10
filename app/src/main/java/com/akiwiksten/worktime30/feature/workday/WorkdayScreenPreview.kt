package com.akiwiksten.worktime30.feature.workday

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme

@Preview(showBackground = true, name = "Workday - Loading")
@Composable
fun PreviewWorkdayLoading() {
    WorkdayPreviewContent(uiState = WorkdayUiState.Loading)
}

@Preview(showBackground = true, name = "Workday - Success New Day")
@Composable
fun PreviewWorkdaySuccessNewDay() {
    WorkdayPreviewContent(
        uiState = WorkdayUiState.Success(
            date = "2026-04-10",
            projectName = "Alpha Site",
            dailyWorkTime = "07:30",
            lunchTime = "00:30",
            workTimeTotal = "132:00",
            balanceTotal = "+03:10",
            isNewDay = true
        ),
        projectName = "Alpha Site"
    )
}

@Preview(showBackground = true, name = "Workday - Success Existing Day")
@Composable
fun PreviewWorkdaySuccessExistingDay() {
    WorkdayPreviewContent(
        uiState = WorkdayUiState.Success(
            date = "2026-04-10",
            projectName = "Beta Support",
            startTime = "08:00",
            endTime = "16:30",
            lunchStart = "11:30",
            lunchEnd = "12:00",
            breakStart = "14:15",
            breakEnd = "14:30",
            workTimeToday = "08:00",
            dailyWorkTime = "07:30",
            lunchTime = "00:30",
            balanceToday = "+00:30",
            workTimeTotal = "140:00",
            balanceTotal = "+04:10",
            isNewDay = false
        ),
        projectName = "Beta Support"
    )
}

@Preview(showBackground = true, name = "Workday - Error")
@Composable
fun PreviewWorkdayError() {
    WorkdayPreviewContent(uiState = WorkdayUiState.Error(message = "Failed to load workday"))
}

@Composable
private fun WorkdayPreviewContent(
    uiState: WorkdayUiState,
    projectName: String? = null
) {
    WorkTime30Theme(dynamicColor = false) {
        WorkdayStateContent(
            padding = PaddingValues(0.dp),
            uiState = uiState,
            projectName = projectName,
            actions = WorkdayScreenActions()
        )
    }
}

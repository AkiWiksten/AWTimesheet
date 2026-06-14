package com.akiwiksten.awtimesheet.feature.workday

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.theme.AWTimesheetTheme
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayActions
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayConfiguration
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayUiState
import com.android.tools.screenshot.PreviewTest

private val PreviewPortraitWidth = 411.dp
private val PreviewConfig = WorkdayConfiguration(
    flexDayWorkType = "Absence-Flex day",
    absencePrefix = "Absence-"
)
private val PreviewWorkdayState = WorkdayUiState.Success(
    date = "2026-05-14",
    workTimeByDate = "09:15",
    workTimeByDateEstimate = "07:30",
    flexTimeByDate = "+01:45",
    initialFlexTimeTotal = "+08:00",
    flexTimeTotal = "+09:45",
    projects = listOf(
        SingleProjectState(
            listIndex = 0,
            date = "2026-05-14",
            projectName = "Alpha Site Visit",
            projectTime = "03:30",
            kilometres = "24",
            allowance = "Meal allowance",
            workType = "Field work"
        ),
        SingleProjectState(
            listIndex = 1,
            date = "2026-05-14",
            projectName = "Beta Planning",
            projectTime = "05:45",
            kilometres = "0",
            allowance = "No allowance",
            workType = "Office"
        ),
        SingleProjectState(
            listIndex = 2,
            date = "2026-05-14",
            projectName = "Gamma Notes"
        )
    ),
    workTypes = listOf("Field work", "Office")
)

private val PreviewActions = WorkdayActions(
    onSelectedItemIndexChange = {},
    onTrackProjectEditorLaunch = { _, _ -> },
    onNavigateToSingleProject = {},
    onRetry = {},
    onSaveSettings = { _, _ -> },
    onDeleteProject = {}
)

@PreviewTest
@Preview(name = "Workday Constrained - Portrait", widthDp = 411, heightDp = 891)
@Composable
fun WorkdayContentConstrainedPortraitPreview() {
    WorkdayContentConstrainedPreviewContent()
}

@PreviewTest
@Preview(name = "Workday Constrained - Landscape", widthDp = 891, heightDp = 411)
@Composable
fun WorkdayContentConstrainedLandscapePreview() {
    WorkdayContentConstrainedPreviewContent()
}

@Composable
private fun WorkdayContentConstrainedPreviewContent() {
    val scrollState = rememberScrollState()
    AWTimesheetTheme(dynamicColor = false) {
        Surface {
            PortraitWidthContainer(
                portraitWidth = PreviewPortraitWidth,
                modifier = Modifier.fillMaxSize()
            ) {
                WorkdayContent(
                    workdayUiState = PreviewWorkdayState,
                    selectedItemIndex = 1,
                    scrollState = scrollState,
                    actions = PreviewActions,
                    config = PreviewConfig
                )
            }
        }
    }
}

@Composable
private fun PortraitWidthContainer(
    portraitWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(modifier = Modifier.width(width = portraitWidth)) {
            content()
        }
    }
}


package com.akiwiksten.awtimesheet.feature.projectdetails

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.theme.AWTimesheetTheme
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsScreenActions
import com.android.tools.screenshot.PreviewTest

private val PreviewPortraitWidth = 411.dp
private val PreviewUiState = ProjectDetailsUiState.Success(
    details = ProjectDetailsState(
        date = "2026-05-14",
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

@PreviewTest
@Preview(name = "ProjectDetails Constrained - Portrait", widthDp = 411, heightDp = 891)
@Composable
fun ProjectDetailsConstrainedPortraitPreview() {
    ProjectDetailsConstrainedPreviewContent()
}

@PreviewTest
@Preview(name = "ProjectDetails Constrained - Landscape", widthDp = 891, heightDp = 411)
@Composable
fun ProjectDetailsConstrainedLandscapePreview() {
    ProjectDetailsConstrainedPreviewContent()
}

@Composable
private fun ProjectDetailsConstrainedPreviewContent() {
    AWTimesheetTheme(dynamicColor = false) {
        Surface {
            PortraitWidthContainer(
                portraitWidth = PreviewPortraitWidth,
                modifier = Modifier.fillMaxSize()
            ) {
                ProjectDetailsStateContent(
                    padding = PaddingValues(0.dp),
                    uiState = PreviewUiState,
                    actions = ProjectDetailsScreenActions(),
                    isConfirmEnabled = true
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


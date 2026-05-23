package com.akiwiksten.awtimesheet.feature.singleproject

import androidx.compose.foundation.layout.Box
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
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.android.tools.screenshot.PreviewTest

private val PreviewPortraitWidth = 411.dp
private const val PREVIEW_DATE = "2026-05-14"
private const val NO_ALLOWANCE = "No allowance"

private val PreviewScreenState = SingleProjectScreenState(
    date = PREVIEW_DATE,
    editedProjectIndex = -1,
    state = SingleProjectState(
        index = 0,
        date = PREVIEW_DATE,
        projectName = "Beta Support",
        projectTime = "03:30",
        kilometres = "18",
        allowance = "Full allowance",
        workType = "Maintenance"
    ),
    isAddMode = false,
    uiState = SingleProjectUiState.Success(
        data = SingleProjectState(
            date = PREVIEW_DATE,
            projectTime = "00:00",
            kilometres = "",
            allowance = NO_ALLOWANCE,
            workType = "Installation"
        ),
        workTimeByDate = "07:45"
    ),
    isConfirmEnabled = true,
    isDuplicateProjectName = false
)

@PreviewTest
@Preview(name = "SingleProject Constrained - Portrait", widthDp = 411, heightDp = 891)
@Composable
fun SingleProjectConstrainedPortraitPreview() {
    SingleProjectConstrainedPreviewContent()
}

@PreviewTest
@Preview(name = "SingleProject Constrained - Landscape", widthDp = 891, heightDp = 411)
@Composable
fun SingleProjectConstrainedLandscapePreview() {
    SingleProjectConstrainedPreviewContent()
}

@Composable
private fun SingleProjectConstrainedPreviewContent() {
    AWTimesheetTheme(dynamicColor = false) {
        Surface {
            PortraitWidthContainer(
                portraitWidth = PreviewPortraitWidth,
                modifier = Modifier.fillMaxSize()
            ) {
                SingleProjectScreenContent(
                    screenState = PreviewScreenState,
                    actions = SingleProjectActions(
                        onStateChange = {},
                        onOpenProjectDetails = {},
                        onConfirm = {}
                    ),
                    hasUnsavedChanges = true,
                    onNavigateBack = {}
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


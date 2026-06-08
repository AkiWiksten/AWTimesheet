package com.akiwiksten.awtimesheet.feature.settings

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
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsActions
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsStateContentState
import com.android.tools.screenshot.PreviewTest

private val PreviewPortraitWidth = 411.dp
private val PreviewUiState = SettingsUiState.Success(
    data = SettingsState(
        name = "Aki Wiksten",
        employer = "WorkTime Oy",
        dailyWorkTimeEstimate = "07:30",
        dailyLunchTimeEstimate = "00:00",
        workTypes = listOf("Installation", "Maintenance", "Meeting")
    ),
    selectedDate = "2026-05-14"
)

@PreviewTest
@Preview(name = "Settings Constrained - Portrait", widthDp = 411, heightDp = 891)
@Composable
fun SettingsConstrainedPortraitPreview() {
    SettingsConstrainedPreviewContent()
}

@PreviewTest
@Preview(name = "Settings Constrained - Landscape", widthDp = 891, heightDp = 411)
@Composable
fun SettingsConstrainedLandscapePreview() {
    SettingsConstrainedPreviewContent()
}

@Composable
private fun SettingsConstrainedPreviewContent() {
    AWTimesheetTheme(dynamicColor = false) {
        Surface {
            PortraitWidthContainer(
                portraitWidth = PreviewPortraitWidth,
                modifier = Modifier.fillMaxSize()
            ) {
                SettingsStateContent(
                    state = SettingsStateContentState(
                        uiState = PreviewUiState,
                        defaultWorkTypes = listOf("Other"),
                        onUnsavedChangesChanged = {},
                        registerUnsavedActions = { _, _ -> },
                        onDiscardChanges = {},
                        createActions = {
                            SettingsActions(
                                onNameChange = {},
                                onEmployerChange = {},
                                onDailyWorkTimeEstimateChange = {},
                                onDailyLunchTimeEstimateChange = {},
                                onInitialFlexTimeTotalChange = {},
                                onWorkTypeAdded = {},
                                onWorkTypeRemoved = {},
                                onSave = {},
                                onGenerateXlsx = {},
                                onGenerateWorkdaysForMonth = {},
                                onGenerateWorkdaysForYear = {}
                            )
                        }
                    )
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

package com.akiwiksten.awtimesheet.feature.settings.timesheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.theme.AWTimesheetTheme
import com.android.tools.screenshot.PreviewTest

private val PreviewPortraitWidth = 411.dp

@PreviewTest
@Preview(name = "Timesheet Constrained - Portrait", widthDp = 411, heightDp = 891)
@Composable
fun TimesheetConstrainedPortraitPreview() {
    TimesheetConstrainedPreviewContent()
}

@PreviewTest
@Preview(name = "Timesheet Constrained - Landscape", widthDp = 891, heightDp = 411)
@Composable
fun TimesheetConstrainedLandscapePreview() {
    TimesheetConstrainedPreviewContent()
}

@Composable
private fun TimesheetConstrainedPreviewContent() {
    AWTimesheetTheme(dynamicColor = false) {
        Surface {
            PortraitWidthContainer(
                portraitWidth = PreviewPortraitWidth,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(text = "Timesheet export")
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


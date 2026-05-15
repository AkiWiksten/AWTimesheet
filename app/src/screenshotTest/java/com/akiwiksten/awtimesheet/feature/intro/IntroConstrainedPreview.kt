package com.akiwiksten.awtimesheet.feature.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.akiwiksten.awtimesheet.core.theme.AWTimesheetTheme
import com.akiwiksten.awtimesheet.navigation.PortraitWidthContainer

private val PreviewPortraitWidth = 411.dp

@PreviewTest
@Preview(name = "Intro Constrained - Portrait", widthDp = 411, heightDp = 891)
@Composable
fun IntroConstrainedPortraitPreview() {
    IntroConstrainedPreviewContent()
}

@PreviewTest
@Preview(name = "Intro Constrained - Landscape", widthDp = 891, heightDp = 411)
@Composable
fun IntroConstrainedLandscapePreview() {
    IntroConstrainedPreviewContent()
}

@Composable
private fun IntroConstrainedPreviewContent() {
    AWTimesheetTheme(dynamicColor = false) {
        Surface {
            PortraitWidthContainer(
                portraitWidth = PreviewPortraitWidth,
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                IntroStateContent(
                    uiState = IntroUiState.Success(appName = "WorkTime 3.0"),
                    onItemClick = {}
                )
            }
        }
    }
}


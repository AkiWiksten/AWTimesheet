package com.akiwiksten.awtimesheet.feature.location

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
import com.android.tools.screenshot.PreviewTest

private val PreviewPortraitWidth = 411.dp

@PreviewTest
@Preview(name = "Distance Calculator Constrained - Portrait", widthDp = 411, heightDp = 891)
@Composable
fun DistanceCalculatorConstrainedPortraitPreview() {
    DistanceCalculatorConstrainedPreviewContent()
}

@PreviewTest
@Preview(name = "Distance Calculator Constrained - Landscape", widthDp = 891, heightDp = 411)
@Composable
fun DistanceCalculatorConstrainedLandscapePreview() {
    DistanceCalculatorConstrainedPreviewContent()
}

@PreviewTest
@Preview(name = "Distance Calculator Constrained Empty - Portrait", widthDp = 411, heightDp = 891)
@Composable
fun DistanceCalculatorConstrainedEmptyPortraitPreview() {
    DistanceCalculatorConstrainedEmptyPreviewContent()
}

@PreviewTest
@Preview(name = "Distance Calculator Constrained Empty - Landscape", widthDp = 891, heightDp = 411)
@Composable
fun DistanceCalculatorConstrainedEmptyLandscapePreview() {
    DistanceCalculatorConstrainedEmptyPreviewContent()
}

@Composable
private fun DistanceCalculatorConstrainedPreviewContent() {
    Surface {
        PortraitWidthContainer(
            portraitWidth = PreviewPortraitWidth,
            modifier = Modifier.fillMaxSize()
        ) {
            PreviewDistanceCalculatorWithHistory()
        }
    }
}

@Composable
private fun DistanceCalculatorConstrainedEmptyPreviewContent() {
    Surface {
        PortraitWidthContainer(
            portraitWidth = PreviewPortraitWidth,
            modifier = Modifier.fillMaxSize()
        ) {
            PreviewDistanceCalculatorEmpty()
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



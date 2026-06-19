package com.akiwiksten.awtimesheet.feature.absence

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
@Preview(name = "Absence Constrained - Portrait", widthDp = 411, heightDp = 891)
@Composable
fun AbsenceConstrainedPortraitPreview() {
    AbsenceConstrainedPreviewContent()
}

@PreviewTest
@Preview(name = "Absence Constrained - Landscape", widthDp = 891, heightDp = 411)
@Composable
fun AbsenceConstrainedLandscapePreview() {
    AbsenceConstrainedPreviewContent()
}

@PreviewTest
@Preview(name = "Absence Constrained Empty - Portrait", widthDp = 411, heightDp = 891)
@Composable
fun AbsenceConstrainedEmptyPortraitPreview() {
    AbsenceConstrainedEmptyPreviewContent()
}

@PreviewTest
@Preview(name = "Absence Constrained Empty - Landscape", widthDp = 891, heightDp = 411)
@Composable
fun AbsenceConstrainedEmptyLandscapePreview() {
    AbsenceConstrainedEmptyPreviewContent()
}

@Composable
private fun AbsenceConstrainedPreviewContent() {
    Surface {
        PortraitWidthContainer(
            portraitWidth = PreviewPortraitWidth,
            modifier = Modifier.fillMaxSize()
        ) {
            PreviewAbsenceWithSelection()
        }
    }
}

@Composable
private fun AbsenceConstrainedEmptyPreviewContent() {
    Surface {
        PortraitWidthContainer(
            portraitWidth = PreviewPortraitWidth,
            modifier = Modifier.fillMaxSize()
        ) {
            PreviewAbsenceEmpty()
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



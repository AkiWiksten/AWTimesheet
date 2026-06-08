package com.akiwiksten.awtimesheet.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.android.tools.screenshot.PreviewTest

private val PreviewPortraitWidth = 411.dp

@PreviewTest
@Preview(name = "Portrait Width Constraint - Portrait", widthDp = 411, heightDp = 891)
@Composable
fun PortraitWidthConstraintPortraitPreview() {
    PortraitWidthConstraintPreviewContent()
}

@PreviewTest
@Preview(name = "Portrait Width Constraint - Landscape", widthDp = 891, heightDp = 411)
@Composable
fun PortraitWidthConstraintLandscapePreview() {
    PortraitWidthConstraintPreviewContent()
}

@Composable
private fun PortraitWidthConstraintPreviewContent() {
    Surface {
        PortraitWidthContainer(
            portraitWidth = PreviewPortraitWidth,
            modifier = Modifier
                .fillMaxSize()
                .background(color = Color(0xFFE9EDF2))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xFFCFD8E3))
                    .padding(all = PADDING_SPACING),
                verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
            ) {
                Text(text = "Portrait constrained content")
                repeat(4) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = Color.White
                    ) {}
                }
            }
        }
    }
}


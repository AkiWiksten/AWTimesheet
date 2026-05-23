package com.akiwiksten.awtimesheet.feature.calendar

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
import com.android.tools.screenshot.PreviewTest
import java.time.YearMonth

private val PreviewPortraitWidth = 411.dp
private val PreviewCalendarState = CalendarUiState.Success(
    date = "2026-05-14",
    timePerMonth = "160:30",
    timePerWeek = "38:15",
    timePerDay = "07:30",
    datesWithWork = setOf("2026-05-05", "2026-05-06", "2026-05-12", "2026-05-14"),
    visibleMonth = YearMonth.of(2026, 5)
)

@PreviewTest
@Preview(name = "Calendar Constrained - Portrait", widthDp = 411, heightDp = 891)
@Composable
fun CalendarContentConstrainedPortraitPreview() {
    CalendarContentConstrainedPreviewContent()
}

@PreviewTest
@Preview(name = "Calendar Constrained - Landscape", widthDp = 891, heightDp = 411)
@Composable
fun CalendarContentConstrainedLandscapePreview() {
    CalendarContentConstrainedPreviewContent()
}

@Composable
private fun CalendarContentConstrainedPreviewContent() {
    AWTimesheetTheme(dynamicColor = false) {
        Surface {
            PortraitWidthContainer(
                portraitWidth = PreviewPortraitWidth,
                modifier = Modifier.fillMaxSize()
            ) {
                CalendarContent(
                    uiState = PreviewCalendarState,
                    onDateSelected = {},
                    onVisibleMonthChanged = {}
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


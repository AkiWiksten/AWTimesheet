package com.akiwiksten.worktime30.feature.calendar

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest

private const val PREVIEW_MONTH_TIME = "170:00 h"
private const val PREVIEW_WEEK_TIME = "42:30 h"
private const val PREVIEW_DAY_TIME = "8:30 h"
private const val PREVIEW_DATE = "2026-04-10"

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PreviewCalendarContentLoading() {
    CalendarContent(
        uiState = CalendarUiState.Loading,
        onDateSelected = {}
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PreviewCalendarContentSuccess() {
    val successState = CalendarUiState.Success(
        date = PREVIEW_DATE,
        timePerDay = PREVIEW_DAY_TIME,
        timePerWeek = PREVIEW_WEEK_TIME,
        timePerMonth = PREVIEW_MONTH_TIME
    )
    CalendarContent(
        uiState = successState,
        onDateSelected = {}
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PreviewCalendarContentError() {
    CalendarContent(
        uiState = CalendarUiState.Error("Failed to load calendar data"),
        onDateSelected = {}
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PreviewWorkTimeSummarySection() {
    WorkTimeSummarySection(
        uiState = CalendarUiState.Success(
            date = PREVIEW_DATE,
            timePerDay = PREVIEW_DAY_TIME,
            timePerWeek = PREVIEW_WEEK_TIME,
            timePerMonth = PREVIEW_MONTH_TIME
        )
    )
}

@PreviewTest
@Preview(showBackground = true)
@Composable
fun PreviewSummaryItem() {
    SummaryItem(
        label = "Work Time Today",
        value = PREVIEW_DAY_TIME
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Summary Item - Week")
@Composable
fun PreviewSummaryItemWeek() {
    SummaryItem(
        label = "Work Time This Week",
        value = PREVIEW_WEEK_TIME
    )
}

@PreviewTest
@Preview(showBackground = true, name = "Summary Item - Month")
@Composable
fun PreviewSummaryItemMonth() {
    SummaryItem(
        label = "Work Time This Month",
        value = PREVIEW_MONTH_TIME
    )
}

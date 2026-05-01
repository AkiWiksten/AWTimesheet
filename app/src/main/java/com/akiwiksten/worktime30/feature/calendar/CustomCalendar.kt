@file:Suppress("kotlin:S1854", "UNUSED_VALUE")

package com.akiwiksten.worktime30.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Suppress("MagicNumber")
private val workDayRed = Color(0xFF8B0000)

private const val DAYS_IN_WEEK = 7
private const val WEEK_GRID_ROUND_UP = 6
private const val YEAR_PICKER_RANGE = 25
private const val YEAR_PICKER_CENTER_OFFSET = 3

private val orderedDaysOfWeek = listOf(
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
    DayOfWeek.SUNDAY
)

@Suppress("kotlin:S1854", "UNUSED_VALUE")
@Composable
fun CustomCalendar(
    selectedDate: LocalDate,
    datesWithWork: Set<String>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    var displayedMonth by remember(selectedDate) {
        mutableStateOf(YearMonth.of(selectedDate.year, selectedDate.month))
    }
    val isYearPickerOpenState = remember { mutableStateOf(false) }
    val today = LocalDate.now()

    Column(modifier = modifier.fillMaxWidth()) {
        MonthHeader(
            displayedMonth = displayedMonth,
            onPrevious = { displayedMonth = displayedMonth.minusMonths(1) },
            onNext = { displayedMonth = displayedMonth.plusMonths(1) },
            onMonthYearClick = { isYearPickerOpenState.value = true }
        )
        DayOfWeekHeader()
        CalendarGrid(
            displayedMonth = displayedMonth,
            selectedDate = selectedDate,
            today = today,
            datesWithWork = datesWithWork,
            onDateSelected = onDateSelected
        )
    }

    if (isYearPickerOpenState.value) {
        val currentYear = displayedMonth.year
        val years = (currentYear - YEAR_PICKER_RANGE..currentYear + YEAR_PICKER_RANGE).toList().reversed()
        YearPickerDialog(
            selectedYear = currentYear,
            years = years,
            onDismiss = { isYearPickerOpenState.value = false },
            onYearSelected = { pickedYear ->
                displayedMonth = YearMonth.of(pickedYear, displayedMonth.month)
                isYearPickerOpenState.value = false
            }
        )
    }
}

@Composable
private fun MonthHeader(
    displayedMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMonthYearClick: () -> Unit
) {
    val monthLabel = displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous month"
            )
        }
        Text(
            text = "$monthLabel ${displayedMonth.year}",
            modifier = Modifier.clickable { onMonthYearClick() },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textDecoration = TextDecoration.Underline
        )
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next month"
            )
        }
    }
}

@Composable
private fun YearPickerDialog(
    selectedYear: Int,
    years: List<Int>,
    onDismiss: () -> Unit,
    onYearSelected: (Int) -> Unit
) {
    val selectedIndex = years.indexOf(selectedYear).coerceAtLeast(0)
    val startIndex = (selectedIndex - YEAR_PICKER_CENTER_OFFSET).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Select year") },
        text = {
            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = 320.dp)
            ) {
                items(years) { year ->
                    TextButton(
                        onClick = { onYearSelected(year) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = year.toString(),
                            fontWeight = if (year == selectedYear) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close")
            }
        }
    )
}

@Composable
private fun DayOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        orderedDaysOfWeek.forEach { dow ->
            Text(
                text = dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun CalendarGrid(
    displayedMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    datesWithWork: Set<String>,
    onDateSelected: (LocalDate) -> Unit
) {
    // Monday-first offset: Monday=1 → 0, Sunday=7 → 6
    val startOffset = displayedMonth.atDay(1).dayOfWeek.value - 1
    val daysInMonth = displayedMonth.lengthOfMonth()
    val rows = (startOffset + daysInMonth + WEEK_GRID_ROUND_UP) / DAYS_IN_WEEK

    for (row in 0 until rows) {
        Row(modifier = Modifier.fillMaxWidth()) {
            for (col in 0 until DAYS_IN_WEEK) {
                val dayNumber = row * DAYS_IN_WEEK + col - startOffset + 1
                if (dayNumber < 1 || dayNumber > daysInMonth) {
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                } else {
                    val date = displayedMonth.atDay(dayNumber)
                    DayCell(
                        day = dayNumber,
                        isSelected = date == selectedDate,
                        isToday = date == today,
                        hasWork = date.toString() in datesWithWork,
                        onClick = { onDateSelected(date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun DayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasWork: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = resolveDayCellBackground(isSelected, isToday)
    val textColor = resolveDayCellTextColor(isSelected, isToday, hasWork)
    val workBorderColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else workDayRed

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .then(
                if (hasWork) {
                    Modifier.border(BorderStroke(1.5.dp, workBorderColor), CircleShape)
                } else {
                    Modifier
                }
            )
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (isSelected || isToday || hasWork) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun resolveDayCellBackground(isSelected: Boolean, isToday: Boolean) = when {
    isSelected -> MaterialTheme.colorScheme.primary
    isToday -> MaterialTheme.colorScheme.primaryContainer
    else -> Color.Transparent
}

@Composable
private fun resolveDayCellTextColor(isSelected: Boolean, isToday: Boolean, hasWork: Boolean) = when {
    isSelected -> MaterialTheme.colorScheme.onPrimary
    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
    hasWork -> workDayRed
    else -> MaterialTheme.colorScheme.onSurface
}

package com.akiwiksten.awtimesheet.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.akiwiksten.awtimesheet.core.HEADER_CONTENT_PADDING
import com.akiwiksten.awtimesheet.core.HEADER_CONTENT_SPACING
import com.akiwiksten.awtimesheet.core.ui.CenteredErrorBox
import com.akiwiksten.awtimesheet.core.ui.CenteredLoadingBox
import com.akiwiksten.awtimesheet.core.ui.Header
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumn
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by calendarViewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // OPTIMIZATION: Start auto-reload AFTER first frame is drawn.
    // This defers database queries off the critical startup path, reducing jank.
    // Placeholder state ensures UI renders instantly without "Loading" spinner.
    LaunchedEffect(Unit) {
        calendarViewModel.startAutoReload()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                calendarViewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    CalendarContent(
        uiState = uiState,
        onDateSelected = { calendarViewModel.onDateSelected(it) },
        onVisibleMonthChanged = { calendarViewModel.onVisibleMonthChanged(it) }
    )
}

@Composable
internal fun CalendarContent(
    uiState: CalendarUiState,
    onDateSelected: (String) -> Unit,
    onVisibleMonthChanged: (YearMonth) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    ScrollableScreenColumn(
        scrollState = scrollState,
        modifier = Modifier.fillMaxSize(),
        columnModifier = Modifier
            .fillMaxWidth()
            .padding(all = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 20.dp)
    ) {
        when (uiState) {
            is CalendarUiState.Loading -> CenteredLoadingBox(modifier = Modifier.fillMaxWidth(), fillMaxSize = false)
            is CalendarUiState.Success -> {
                CalendarHeaderSection(selectedDate = uiState.date)
                ElevatedCard(
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CustomCalendar(
                        selectedDate = LocalDate.parse(uiState.date),
                        datesWithWork = uiState.datesWithWork,
                        onDateSelected = { onDateSelected(it.toString()) },
                        modifier = Modifier.padding(all = 8.dp),
                        monthConfig = CalendarVisibleMonthConfig(
                            visibleMonth = uiState.visibleMonth,
                            onVisibleMonthChanged = onVisibleMonthChanged
                        )
                    )
                }
                WorkTimeSummarySection(uiState = uiState)
            }
            is CalendarUiState.Error -> CenteredErrorBox(
                errorMessage = uiState.message,
                modifier = Modifier.fillMaxWidth(),
                fillMaxSize = false
            )
        }
    }
}

@Composable
private fun CalendarHeaderSection(selectedDate: String) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(all = HEADER_CONTENT_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = HEADER_CONTENT_SPACING)
        ) {
            Header(title = stringResource(id = R.string.select_work_day_date))

            OutlinedTextField(
                value = selectedDate,
                onValueChange = { },
                label = {
                    Text(
                        text = stringResource(id = R.string.selected_date),
                        fontSize = 20.sp
                    )
                },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = stringResource(id = R.string.select_date)
                    )
                },
                textStyle = TextStyle.Default.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun WorkTimeSummarySection(uiState: CalendarUiState.Success) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .padding(vertical = 20.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 10.dp)
        ) {
            SummaryItem(
                label = stringResource(id = R.string.selected_work_day),
                value = uiState.timePerDay
            )
            SummaryItem(
                label = stringResource(id = R.string.selected_work_week),
                value = uiState.timePerWeek
            )
            SummaryItem(
                label = stringResource(id = R.string.selected_work_month),
                value = uiState.timePerMonth
            )
        }
    }
}

@Composable
internal fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 20.sp
        )
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

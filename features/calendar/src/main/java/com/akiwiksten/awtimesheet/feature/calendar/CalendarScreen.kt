package com.akiwiksten.awtimesheet.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.ui.CenteredErrorBox
import com.akiwiksten.awtimesheet.core.ui.CenteredLoadingBox
import com.akiwiksten.awtimesheet.core.ui.Header
import com.akiwiksten.awtimesheet.core.ui.LocalContentBottomPadding
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumn
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumnState
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by calendarViewModel.uiState.collectAsStateWithLifecycle()
    val isInitialLoadComplete by calendarViewModel.isInitialLoadComplete.collectAsStateWithLifecycle()
    val latestIsInitialLoadComplete by rememberUpdatedState(isInitialLoadComplete)
    val lifecycleOwner = LocalLifecycleOwner.current
    val absencePrefix = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.absence_prefix)

    LaunchedEffect(absencePrefix) {
        calendarViewModel.setLocalizedAbsencePrefix(absencePrefix)
    }

    // OPTIMIZATION: Start auto-reload AFTER first frame is drawn.
    // This defers database queries off the critical startup path, reducing jank.
    // Wait two frames to avoid immediate contention right after first draw.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        withFrameNanos { }
        calendarViewModel.startAutoReload()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && latestIsInitialLoadComplete) {
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
        onVisibleMonthChanged = { calendarViewModel.onVisibleMonthChanged(it) },
    )
}

@Composable
internal fun CalendarContent(
    uiState: CalendarUiState,
    onDateSelected: (String) -> Unit,
    onVisibleMonthChanged: (YearMonth) -> Unit = {},
) {
    val scrollState = rememberScrollState()

    ScrollableScreenColumn(
        state = ScrollableScreenColumnState(
            scrollState = scrollState,
            modifier = Modifier.fillMaxSize(),
            columnModifier = Modifier
                .fillMaxWidth()
                .padding(all = PADDING_SPACING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING)
        )
    ) {
        when (uiState) {
            is CalendarUiState.Initial -> CenteredLoadingBox(modifier = Modifier.fillMaxWidth(), fillMaxSize = false)
            is CalendarUiState.Loading -> CenteredLoadingBox(modifier = Modifier.fillMaxWidth(), fillMaxSize = false)
            is CalendarUiState.Success -> {
                CalendarHeaderSection(selectedDate = uiState.date)
                ElevatedCard(
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CustomCalendar(
                        selectedDate = LocalDate.parse(uiState.date),
                        onDateSelected = { onDateSelected(it.toString()) },
                        markers = CalendarMarkers(
                            datesWithWork = uiState.datesWithWork,
                            datesWithAbsence = uiState.datesWithAbsence
                        ),
                        modifier = Modifier.padding(all = PADDING_SPACING),
                        monthConfig = CalendarVisibleMonthConfig(
                            visibleMonth = uiState.visibleMonth,
                            onVisibleMonthChanged = onVisibleMonthChanged
                        )
                    )
                }
                WorkTimeSummarySection(uiState = uiState)
                Spacer(modifier = Modifier.padding(bottom = LocalContentBottomPadding.current))
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
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(all = PADDING_SPACING_SMALL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
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
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = PADDING_SPACING)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING)
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

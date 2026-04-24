package com.akiwiksten.worktime30.feature.calendar

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.HEADER_CONTENT_PADDING
import com.akiwiksten.worktime30.core.HEADER_CONTENT_SPACING
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.worktime30.core.ui.verticalScrollbar
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by calendarViewModel.uiState.collectAsState()
    val currentUiState = uiState // Store in local variable for smart cast

    val configuration = createMondayFirstConfiguration()

    CompositionLocalProvider(LocalConfiguration provides configuration) {
        val datePickerState = rememberDatePickerState()

        // Sync the date picker to the ViewModel's date whenever the state first becomes Success
        // (or changes to a new Success value, e.g. after a background recalculation).
        LaunchedEffect(key1 = currentUiState) {
            if (currentUiState is CalendarUiState.Success) {
                val millis = LocalDate.parse(currentUiState.date)
                    .atStartOfDay(ZoneId.of("UTC"))
                    .toInstant()
                    .toEpochMilli()
                if (datePickerState.selectedDateMillis != millis) {
                    datePickerState.selectedDateMillis = millis
                }
            }
        }

        LaunchedEffect(key1 = datePickerState.selectedDateMillis) {
            datePickerState.selectedDateMillis?.let {
                val selectedDate = calendarViewModel.convertMillisToDate(millis = it)
                val currentDate = (currentUiState as? CalendarUiState.Success)?.date ?: ""
                if (selectedDate != currentDate) {
                    calendarViewModel.onDateSelected(selectedDate = selectedDate)
                }
            }
        }

        CalendarContent(
            uiState = currentUiState,
            datePickerState = datePickerState
        )
    }
}

@Composable
private fun createMondayFirstConfiguration(): Configuration {
    // Get the current application locale from the configuration
    val appLocale = LocalConfiguration.current.locales[0]

    // Create a modified locale that uses the app's language/country
    // but ensures Monday is the first day of the week (fw=mon)
    val mondayFirstLocale = remember(appLocale) {
        Locale.Builder()
            .setLocale(appLocale)
            .setUnicodeLocaleKeyword("fw", "mon")
            .build()
    }

    return Configuration(LocalConfiguration.current).apply {
        setLocale(mondayFirstLocale)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CalendarContent(
    uiState: CalendarUiState,
    datePickerState: DatePickerState
) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = uiState is CalendarUiState.Loading
    )
    val scrollState = rememberScrollState()
    var lastSuccessState by remember { mutableStateOf<CalendarUiState.Success?>(value = null) }
    val fallbackSuccessState = remember { CalendarUiState.Success() }

    LaunchedEffect(uiState) {
        if (uiState is CalendarUiState.Success) {
            lastSuccessState = uiState
        }
    }

    Column(
        modifier = Modifier
            .verticalScrollbar(scrollState = scrollState)
            .verticalScroll(state = scrollState)
            .fillMaxWidth()
            .padding(all = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 20.dp)
    ) {
        when (uiState) {
            is CalendarUiState.Loading -> LoadingContent(
                showLoadingIndicator = showLoadingIndicator,
                cachedState = lastSuccessState ?: fallbackSuccessState,
                datePickerState = datePickerState
            )
            is CalendarUiState.Success -> {
                DatePickerSection(selectedDate = uiState.date, datePickerState = datePickerState)
                WorkTimeSummarySection(uiState = uiState)
            }
            is CalendarUiState.Error -> ErrorContent(message = uiState.message)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingContent(
    showLoadingIndicator: Boolean,
    cachedState: CalendarUiState.Success,
    datePickerState: DatePickerState
) {
    if (showLoadingIndicator) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        DatePickerSection(selectedDate = cachedState.date, datePickerState = datePickerState)
        WorkTimeSummarySection(uiState = cachedState)
    }
}

@Composable
private fun ErrorContent(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Error: $message",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(all = 32.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerSection(
    selectedDate: String,
    datePickerState: DatePickerState
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 14.dp),
        modifier = Modifier.padding(all = 2.dp)
    ) {
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
                    modifier = Modifier.height(height = 64.dp),
                    textStyle = TextStyle.Default.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                )
            }
        }

        Box(
            modifier = Modifier
                .shadow(elevation = 8.dp)
                .background(color = MaterialTheme.colorScheme.surface)
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = true,
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

package com.akiwiksten.awtimesheet.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.core.DATE_FORMAT
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.usecase.CalendarData
import com.akiwiksten.awtimesheet.domain.usecase.GetCalendarDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for the Calendar screen, managing date selection and work time summaries.
 */
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getCalendarDataUseCase: GetCalendarDataUseCase,
    private val dateRepository: DateRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
    private val today = LocalDate.now().format(dateFormatter)

    // Show placeholder state immediately on startup (no Loading spinner)
    private val initialPlaceholder = CalendarUiState.Success(
        date = today,
        timePerDay = "0:00",
        timePerWeek = "0:00",
        timePerMonth = "0:00",
        datesWithWork = emptySet(),
        visibleMonth = YearMonth.now()
    )

    private val _uiState = MutableStateFlow<CalendarUiState>(initialPlaceholder)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    // Cache for CalendarData to avoid re-fetching when returning to the screen
    private val calendarDataCache = mutableMapOf<String, CalendarData>()

    private val _isInitialLoadComplete = MutableStateFlow(false)
    val isInitialLoadComplete: StateFlow<Boolean> = _isInitialLoadComplete.asStateFlow()

    init {
        // OPTIMIZATION: Defer date collection to LaunchedEffect in Composable.
        // This allows first frame to render without blocking on DB queries.
        // See CalendarScreen for the LaunchedEffect that resumes collection.
    }

    fun startAutoReload() {
        viewModelScope.launch {
            dateRepository.selectedDate.collect { date ->
                if (date.isNotEmpty()) {
                    refreshCalendarData(
                        date = date,
                        showLoading = _uiState.value !is CalendarUiState.Success
                    )
                }
            }
        }
    }

    /**
     * Formats milliseconds into a date string (yyyy-MM-dd).
     */
    fun convertMillisToDate(millis: Long): String {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
    }

    /**
     * Re-loads data for the currently selected date.
     */
    fun refresh() {
        viewModelScope.launch {
            val selectedDate = (_uiState.value as? CalendarUiState.Success)?.date
                ?: dateRepository.selectedDate.value
            if (selectedDate.isNotEmpty()) {
                refreshCalendarData(
                    date = selectedDate,
                    showLoading = false
                )
            }
        }
    }

    /**
     * Updates marker days for the currently visible calendar month without changing selected date.
     */
    fun onVisibleMonthChanged(month: YearMonth) {
        val currentState = _uiState.value as? CalendarUiState.Success ?: return

        viewModelScope.launch {
            try {
                val monthDate = month.atDay(1).toString()
                // Check cache first before fetching
                val monthData = calendarDataCache[monthDate] ?: run {
                    val fetchedData = getCalendarDataUseCase(monthDate)
                    calendarDataCache[monthDate] = fetchedData
                    fetchedData
                }

                _uiState.value = currentState.copy(
                    datesWithWork = monthData.datesWithWork,
                    visibleMonth = month
                )
            } catch (_: IllegalArgumentException) {
                // Keep current UI state if month marker refresh fails.
            } catch (_: IllegalStateException) {
                // Keep current UI state if month marker refresh fails.
            }
        }
    }

    /**
     * Updates the currently selected date and recalculates sums.
     */
    fun onDateSelected(selectedDate: String) {
        dateRepository.updateDate(selectedDate)
    }

    private suspend fun refreshCalendarData(date: String, showLoading: Boolean) {
        if (showLoading) {
            _uiState.value = CalendarUiState.Loading
        }

        try {
            val workTimeByDateChange = dateRepository.workTimeByDateChange.value
            val cachedData = calendarDataCache[date]
            val data: CalendarData = cachedData ?: run {
                val fetchedData = getCalendarDataUseCase(date)
                calendarDataCache[date] = fetchedData
                fetchedData
            }

            val dataToDisplay = if (cachedData != null && workTimeByDateChange != ZERO_TIME) {
                data.copy(
                    timePerDay = addWorkTimeChange(data.timePerDay, workTimeByDateChange),
                    timePerWeek = addWorkTimeChange(data.timePerWeek, workTimeByDateChange),
                    timePerMonth = addWorkTimeChange(data.timePerMonth, workTimeByDateChange)
                ).also { adjustedData ->
                    calendarDataCache[date] = adjustedData
                }
            } else {
                data
            }

            val month = YearMonth.from(LocalDate.parse(date))
            _uiState.value = CalendarUiState.Success(
                date = date,
                timePerMonth = dataToDisplay.timePerMonth,
                timePerWeek = dataToDisplay.timePerWeek,
                timePerDay = dataToDisplay.timePerDay,
                datesWithWork = dataToDisplay.datesWithWork,
                visibleMonth = month
            )

            if (workTimeByDateChange != ZERO_TIME) {
                dateRepository.updateWorkTimeByDateChange(ZERO_TIME)
            }
        } catch (e: IllegalArgumentException) {
            _uiState.value = CalendarUiState.Error(e.message ?: "Invalid argument provided")
        } catch (e: IllegalStateException) {
            _uiState.value = CalendarUiState.Error(e.message ?: "Invalid state")
        }
    }

    private fun addWorkTimeChange(baseTime: String, change: String): String {
        return if (change == ZERO_TIME) {
            baseTime
        } else {
            WorkTimeCalculator.calculateFlexTime(baseTime, change)
        }
    }
}

sealed class CalendarUiState {
    object Loading : CalendarUiState()

    data class Success(
        val date: String = "",
        val timePerMonth: String = "",
        val timePerWeek: String = "",
        val timePerDay: String = "",
        val datesWithWork: Set<String> = emptySet(),
        val visibleMonth: YearMonth = YearMonth.now()
    ) : CalendarUiState()

    data class Error(val message: String) : CalendarUiState()
}

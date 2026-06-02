package com.akiwiksten.awtimesheet.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.awtimesheet.core.DATE_FORMAT
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.usecase.GetCalendarDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    // Keep startup UI minimal to reduce first-draw cost.
    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Initial)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val _isInitialLoadComplete = MutableStateFlow(false)
    val isInitialLoadComplete: StateFlow<Boolean> = _isInitialLoadComplete.asStateFlow()

    init {
        // OPTIMIZATION: Defer date collection to LaunchedEffect in Composable.
        // This allows first frame to render without blocking on DB queries.
        // See CalendarScreen for the LaunchedEffect that resumes collection.
    }

    fun startAutoReload() {
        viewModelScope.launch {
            var lastDate = ""
            var lastRefreshVersion = -1L
            dateRepository.selectedDate
                .combine(dateRepository.calendarRefreshVersion) { date, refreshVersion ->
                    date to refreshVersion
                }
                .collect { (date, refreshVersion) ->
                if (date.isNotEmpty()) {
                    val dateChanged = date != lastDate
                    lastDate = date
                    val forceMonthRecalculation =
                        dateChanged || (refreshVersion != lastRefreshVersion && refreshVersion > 0L)
                    lastRefreshVersion = refreshVersion
                    refreshCalendarData(
                        date = date,
                        showLoading = _uiState.value !is CalendarUiState.Success,
                        forceMonthRecalculation = forceMonthRecalculation
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
                    showLoading = false,
                    forceMonthRecalculation = false
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
                val monthData = getCalendarDataUseCase(monthDate)

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
        if (selectedDate == dateRepository.selectedDate.value) {
            viewModelScope.launch {
                refreshCalendarData(
                    date = selectedDate,
                    showLoading = false,
                    forceMonthRecalculation = true
                )
            }
            return
        }

        dateRepository.updateDate(selectedDate)
    }

    private suspend fun refreshCalendarData(
        date: String,
        showLoading: Boolean,
        forceMonthRecalculation: Boolean,
    ) {
        if (showLoading) {
            _uiState.value = CalendarUiState.Loading
        }

        try {
            val workTimeByDateChange = dateRepository.workTimeByDateChange.value

            val currentState = _uiState.value as? CalendarUiState.Success
            val incrementalChange = if (currentState != null && !forceMonthRecalculation) {
                workTimeByDateChange
            } else {
                ZERO_TIME
            }
            if (
                !forceMonthRecalculation &&
                workTimeByDateChange == ZERO_TIME &&
                currentState?.date == date
            ) {
                return
            }

            val baseData = getCalendarDataUseCase(
                date = date,
                workTimeByDateChange = incrementalChange,
                forceMonthRecalculation = forceMonthRecalculation
            )

            val month = YearMonth.from(LocalDate.parse(date))
            _uiState.value = CalendarUiState.Success(
                date = date,
                timePerMonth = baseData.timePerMonth,
                timePerWeek = baseData.timePerWeek,
                timePerDay = baseData.timePerDay,
                datesWithWork = baseData.datesWithWork,
                visibleMonth = month
            )
            _isInitialLoadComplete.value = true

            if (workTimeByDateChange != ZERO_TIME) {
                dateRepository.updateWorkTimeByDateChange(ZERO_TIME)
            }
        } catch (e: IllegalArgumentException) {
            _uiState.value = CalendarUiState.Error(e.message ?: "Invalid argument provided")
        } catch (e: IllegalStateException) {
            _uiState.value = CalendarUiState.Error(e.message ?: "Invalid state")
        }
    }

}

sealed class CalendarUiState {
    object Initial : CalendarUiState()
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

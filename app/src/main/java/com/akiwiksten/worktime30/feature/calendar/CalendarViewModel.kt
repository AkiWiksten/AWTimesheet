package com.akiwiksten.worktime30.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.DATE_FORMAT
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.usecase.CalendarData
import com.akiwiksten.worktime30.domain.usecase.GetCalendarDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
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

    private val _uiState = MutableStateFlow<CalendarUiState>(CalendarUiState.Loading)
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT)

    init {
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
     * Updates marker days for the currently visible calendar month without changing selected date.
     */
    fun onVisibleMonthChanged(month: YearMonth) {
        val currentState = _uiState.value as? CalendarUiState.Success ?: return

        viewModelScope.launch {
            try {
                val monthDate = month.atDay(1).toString()
                val monthData = getCalendarDataUseCase(monthDate)

                _uiState.value = currentState.copy(
                    datesWithWork = monthData.datesWithWork
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
            val data: CalendarData = getCalendarDataUseCase(date)
            _uiState.value = CalendarUiState.Success(
                date = date,
                timePerMonth = data.timePerMonth,
                timePerWeek = data.timePerWeek,
                timePerDay = data.timePerDay,
                datesWithWork = data.datesWithWork
            )
        } catch (e: IllegalArgumentException) {
            _uiState.value = CalendarUiState.Error(e.message ?: "Invalid argument provided")
        } catch (e: IllegalStateException) {
            _uiState.value = CalendarUiState.Error(e.message ?: "Invalid state")
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
        val datesWithWork: Set<String> = emptySet()
    ) : CalendarUiState()

    data class Error(val message: String) : CalendarUiState()
}

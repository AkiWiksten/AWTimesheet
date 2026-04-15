package com.akiwiksten.worktime30.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.DATE_FORMAT
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.domain.CalendarData
import com.akiwiksten.worktime30.domain.GetCalendarDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
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
                _uiState.value = CalendarUiState.Loading
                calculateSums(date)
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
     * Updates the currently selected date and recalculates sums.
     */
    fun onDateSelected(selectedDate: String) {
        dateRepository.updateDate(selectedDate)
    }

    private fun calculateSums(date: String) {
        viewModelScope.launch {
            try {
                val data: CalendarData = getCalendarDataUseCase(date)
                _uiState.value = CalendarUiState.Success(
                    date = date,
                    timePerMonth = data.timePerMonth,
                    timePerWeek = data.timePerWeek,
                    timePerDay = data.timePerDay
                )
            } catch (e: IllegalArgumentException) {
                _uiState.value = CalendarUiState.Error(e.message ?: "Invalid argument provided")
            } catch (e: IllegalStateException) {
                _uiState.value = CalendarUiState.Error(e.message ?: "Invalid state")
            }
        }
    }
}

sealed class CalendarUiState {
    object Loading : CalendarUiState()

    data class Success(
        val date: String = "",
        val timePerMonth: String = "",
        val timePerWeek: String = "",
        val timePerDay: String = ""
    ) : CalendarUiState()

    data class Error(val message: String) : CalendarUiState()
}

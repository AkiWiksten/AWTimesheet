package com.akiwiksten.worktime30.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.domain.CalendarData
import com.akiwiksten.worktime30.domain.GetCalendarDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    init {
        viewModelScope.launch {
            dateRepository.selectedDate.collect { date ->
                _uiState.update { it.copy(date = date) }
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
            val data: CalendarData = withContext(Dispatchers.IO) {
                getCalendarDataUseCase(date)
            }
            _uiState.update {
                it.copy(
                    timePerMonth = data.timePerMonth,
                    timePerWeek = data.timePerWeek,
                    timePerDay = data.timePerDay,
                    workDaysMonth = data.workDaysMonth
                )
            }
        }
    }
}

data class CalendarUiState(
    val date: String = "",
    val timePerMonth: String = "",
    val timePerWeek: String = "",
    val timePerDay: String = "",
    val workDaysMonth: List<WorkDayEntity> = emptyList()
)

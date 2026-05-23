package com.akiwiksten.awtimesheet.domain.repository

import com.akiwiksten.awtimesheet.core.DATE_FORMAT
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("unused")
class DateRepository @Inject constructor() {
    private val dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
    private val today = LocalDate.now().format(dateFormatter)

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _workTimeByDateChange = MutableStateFlow(ZERO_TIME)
    val workTimeByDateChange: StateFlow<String> = _workTimeByDateChange.asStateFlow()

    fun updateDate(date: String) {
        if (date.isNotEmpty()) {
            _workTimeByDateChange.value = ZERO_TIME
            _selectedDate.value = date
        }
    }

    fun updateWorkTimeByDateChange(change: String) {
        _workTimeByDateChange.value = change
    }

    fun addWorkTimeByDateChange(change: String) {
        _workTimeByDateChange.value = WorkTimeCalculator.calculateFlexTime(
            initialTime = _workTimeByDateChange.value,
            addedTime = change
        )
    }
}

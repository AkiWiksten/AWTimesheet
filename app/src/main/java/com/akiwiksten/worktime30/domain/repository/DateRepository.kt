package com.akiwiksten.worktime30.domain.repository

import com.akiwiksten.worktime30.core.DATE_FORMAT
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateRepository @Inject constructor() {
    private val dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
    private val today = LocalDate.now().format(dateFormatter)

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    fun updateDate(date: String) {
        if (date.isNotEmpty()) {
            _selectedDate.value = date
        }
    }
}

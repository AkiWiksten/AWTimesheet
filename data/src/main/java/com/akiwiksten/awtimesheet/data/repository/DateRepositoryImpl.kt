package com.akiwiksten.awtimesheet.data.repository

import com.akiwiksten.awtimesheet.core.DATE_FORMAT
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DateRepositoryImpl @Inject constructor() : DateRepository {
	private val dateFormatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
	private val today = LocalDate.now().format(dateFormatter)

	private val _selectedDate = MutableStateFlow(today)
	override val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

	private val _workTimeByDateChange = MutableStateFlow(ZERO_TIME)
	override val workTimeByDateChange: StateFlow<String> = _workTimeByDateChange.asStateFlow()

	private val _calendarRefreshVersion = MutableStateFlow(0L)
	override val calendarRefreshVersion: StateFlow<Long> = _calendarRefreshVersion.asStateFlow()

	override fun updateDate(date: String) {
		if (date.isNotEmpty()) {
			_workTimeByDateChange.value = ZERO_TIME
			_selectedDate.value = date
		}
	}

	override fun updateWorkTimeByDateChange(change: String) {
		_workTimeByDateChange.value = change
	}

	override fun addWorkTimeByDateChange(change: String) {
		_workTimeByDateChange.value = WorkTimeCalculator.calculateFlexTime(
			initialTime = _workTimeByDateChange.value,
			addedTime = change
		)
	}

	override fun notifyCalendarDataChanged() {
		_calendarRefreshVersion.value += 1
	}
}
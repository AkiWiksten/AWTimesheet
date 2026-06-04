package com.akiwiksten.awtimesheet.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface DateRepository {
    val selectedDate: StateFlow<String>
    val workTimeByDateChange: StateFlow<String>
    val calendarRefreshVersion: StateFlow<Long>

    fun updateDate(date: String)
    fun updateWorkTimeByDateChange(change: String)
    fun addWorkTimeByDateChange(change: String)
    fun notifyCalendarDataChanged()
}

package com.akiwiksten.awtimesheet.domain.repository

interface WorkdayRepository {
    suspend fun loadWorkday(date: String): String?
    suspend fun upsertWorkdayStats(date: String, workTimeByDateEstimate: String)
    suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow>
}

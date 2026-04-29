package com.akiwiksten.worktime30.domain.repository

import com.akiwiksten.worktime30.domain.model.SettingsState

interface WorkdayRepository {
    suspend fun loadWorkday(date: String): String?
    suspend fun upsertWorkdayStats(date: String, workTimeByDateEstimate: String)
    suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow>
}

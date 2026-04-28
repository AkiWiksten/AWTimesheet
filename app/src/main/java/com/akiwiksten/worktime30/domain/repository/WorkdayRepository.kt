package com.akiwiksten.worktime30.domain.repository

import com.akiwiksten.worktime30.domain.model.WorkStatsState

interface WorkdayRepository {
    suspend fun loadWorkday(date: String): WorkStatsState?
    suspend fun upsertWorkdayStats(date: String, workStats: WorkStatsState)
    suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow>
}

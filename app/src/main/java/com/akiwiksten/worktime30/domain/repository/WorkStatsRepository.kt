package com.akiwiksten.worktime30.domain.repository

import com.akiwiksten.worktime30.domain.model.WorkStatsState

interface WorkStatsRepository {
    suspend fun getWorkStats(): WorkStatsState?
    suspend fun insertWorkStats(workStats: WorkStatsState)
    suspend fun getWorkStatsByDate(date: String): WorkStatsState?
}

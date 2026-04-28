package com.akiwiksten.worktime30.domain.repository

import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.WorkStatsState

interface SettingsRepository {
    suspend fun getSettings(): SettingsState?
    suspend fun insertSettings(settings: SettingsState)
    suspend fun getWorkStats(): WorkStatsState?
    suspend fun insertWorkStats(workStats: WorkStatsState)
    suspend fun getWorkStatsByDate(date: String): WorkStatsState?
    suspend fun getWorkTypes(): List<String>
    suspend fun insertWorkType(workType: String)
    suspend fun deleteWorkType(workType: String)
    suspend fun clearWorkTypes()
}

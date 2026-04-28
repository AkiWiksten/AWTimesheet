package com.akiwiksten.worktime30.domain.repository

import com.akiwiksten.worktime30.domain.model.SettingsState

interface SettingsRepository {
    suspend fun getSettings(): SettingsState?
    suspend fun insertSettings(settings: SettingsState)
    suspend fun getWorkStats(): SettingsState?
    suspend fun insertWorkStats(workStats: SettingsState)
    suspend fun getWorkStatsByDate(date: String): SettingsState?
    suspend fun getWorkTypes(): List<String>
    suspend fun insertWorkType(workType: String)
    suspend fun deleteWorkType(workType: String)
    suspend fun clearWorkTypes()
}

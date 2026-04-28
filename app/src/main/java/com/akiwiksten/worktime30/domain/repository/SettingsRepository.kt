package com.akiwiksten.worktime30.domain.repository

import com.akiwiksten.worktime30.domain.model.SettingsState

interface SettingsRepository {
    suspend fun getSettings(): SettingsState?
    suspend fun insertSettings(settings: SettingsState)
    suspend fun getGlobalSettingsEstimates(): SettingsState?
    suspend fun saveGlobalSettingsEstimates(estimates: SettingsState)
    suspend fun getEffectiveSettingsForDate(date: String): SettingsState?
    suspend fun getWorkTypes(): List<String>
    suspend fun insertWorkType(workType: String)
    suspend fun deleteWorkType(workType: String)
    suspend fun clearWorkTypes()
}

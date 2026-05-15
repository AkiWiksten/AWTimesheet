package com.akiwiksten.awtimesheet.domain.repository

import com.akiwiksten.awtimesheet.domain.model.SettingsState

interface SettingsRepository {
    suspend fun getSettings(): SettingsState?
    suspend fun insertSettings(settings: SettingsState)
    suspend fun getEffectiveSettingsForDate(date: String): SettingsState?
    suspend fun getWorkTypes(): List<String>
    suspend fun insertWorkType(workType: String)
    suspend fun deleteWorkType(workType: String)
    suspend fun deleteAllWorkTypes()
}

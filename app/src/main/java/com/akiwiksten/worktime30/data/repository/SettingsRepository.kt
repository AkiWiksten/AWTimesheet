package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.feature.settings.SettingsState

interface SettingsRepository {
    suspend fun getSettings(): SettingsState?
    suspend fun insertSettings(settings: SettingsState)
    suspend fun getWorkTypes(): List<WorkTypeEntity>
    suspend fun insertWorkType(workType: WorkTypeEntity)
    suspend fun deleteWorkType(workType: WorkTypeEntity)
    suspend fun clearWorkTypes()
}

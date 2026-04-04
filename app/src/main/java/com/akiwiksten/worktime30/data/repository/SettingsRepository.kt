package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity

interface SettingsRepository {
    suspend fun getSettings(): SettingsEntity?
    suspend fun insertSettings(settings: SettingsEntity)
    suspend fun getWorkTypes(): List<WorkTypeEntity>
    suspend fun insertWorkType(workType: WorkTypeEntity)
    suspend fun deleteWorkType(workType: WorkTypeEntity)
    suspend fun clearWorkTypes()
}

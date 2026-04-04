package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.Settings
import com.akiwiksten.worktime30.data.database.WorkType

interface SettingsRepository {
    suspend fun getSettings(): Settings?
    suspend fun insertSettings(settings: Settings)
    suspend fun getWorkTypes(): List<WorkType>
    suspend fun insertWorkType(workType: WorkType)
    suspend fun deleteWorkType(workType: WorkType)
    suspend fun clearWorkTypes()
}

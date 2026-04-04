package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.Settings
import com.akiwiksten.worktime30.data.database.SettingsDao
import com.akiwiksten.worktime30.data.database.WorkType
import com.akiwiksten.worktime30.data.database.WorkTypeDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val workTypeDao: WorkTypeDao
) : SettingsRepository {
    override suspend fun getSettings(): Settings? = settingsDao.loadSettings()
    override suspend fun insertSettings(settings: Settings) = settingsDao.insertSettings(settings)
    override suspend fun getWorkTypes(): List<WorkType> = workTypeDao.loadWorkTypes()
    override suspend fun insertWorkType(workType: WorkType) = workTypeDao.insertWorkType(workType)
    override suspend fun deleteWorkType(workType: WorkType) = workTypeDao.delete(workType)
    override suspend fun clearWorkTypes() = workTypeDao.deleteAll()
}
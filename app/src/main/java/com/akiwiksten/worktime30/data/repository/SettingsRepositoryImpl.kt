package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.SettingsDao
import com.akiwiksten.worktime30.data.database.dao.WorkTypeDao
import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val workTypeDao: WorkTypeDao
) : SettingsRepository {
    override suspend fun getSettings(): SettingsEntity? = settingsDao.loadSettings()
    override suspend fun insertSettings(settings: SettingsEntity) = settingsDao.insertSettings(settings)
    override suspend fun getWorkTypes(): List<WorkTypeEntity> = workTypeDao.loadWorkTypes()
    override suspend fun insertWorkType(workType: WorkTypeEntity) = workTypeDao.insertWorkType(workType)
    override suspend fun deleteWorkType(workType: WorkTypeEntity) = workTypeDao.delete(workType)
    override suspend fun clearWorkTypes() = workTypeDao.deleteAll()
}
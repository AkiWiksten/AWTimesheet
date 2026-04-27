package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.SettingsDao
import com.akiwiksten.worktime30.data.database.dao.WorkTypeDao
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.data.database.mapper.toWorkTypeEntity
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.feature.settings.SettingsState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val workTypeDao: WorkTypeDao
) : SettingsRepository {
    override suspend fun getSettings(): SettingsState? = settingsDao.loadSettings()?.toDomain()
    override suspend fun insertSettings(settings: SettingsState) = settingsDao.insertSettings(settings.toEntity())
    override suspend fun getWorkTypes(): List<String> = workTypeDao.loadWorkTypes().map { it.toDomain() }
    override suspend fun insertWorkType(workType: String) = workTypeDao.insertWorkType(workType.toWorkTypeEntity())
    override suspend fun deleteWorkType(workType: String) = workTypeDao.delete(workType.toWorkTypeEntity())
    override suspend fun clearWorkTypes() = workTypeDao.deleteAll()
}

package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.dao.SettingsDao
import com.akiwiksten.worktime30.data.database.dao.WorkTypeDao
import com.akiwiksten.worktime30.data.database.dao.WorkdayDao
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.data.database.mapper.toWorkTypeEntity
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val workdayDao: WorkdayDao,
    private val workTypeDao: WorkTypeDao
) : SettingsRepository {
    override suspend fun getSettings(): SettingsState? = settingsDao.loadSettings()?.toDomain()
    override suspend fun insertSettings(settings: SettingsState) {
        val existing = settingsDao.loadSettings()
        settingsDao.insertSettings(
            settings.toEntity().copy(
                dailyWorkTimeEstimate = existing?.dailyWorkTimeEstimate ?: settings.dailyWorkTimeEstimate,
                dailyLunchTimeEstimate = existing?.dailyLunchTimeEstimate ?: settings.dailyLunchTimeEstimate,
                initialFlexTimeTotal = existing?.initialFlexTimeTotal ?: settings.initialFlexTimeTotal
            )
        )
    }

    override suspend fun getWorkStats(): SettingsState? =
        settingsDao.loadSettings()?.toDomain()

    override suspend fun insertWorkStats(workStats: SettingsState) {
        val existing = settingsDao.loadSettings()?.toDomain() ?: SettingsState()
        settingsDao.insertSettings(
            existing.copy(
                dailyWorkTimeEstimate = workStats.dailyWorkTimeEstimate,
                dailyLunchTimeEstimate = workStats.dailyLunchTimeEstimate,
                initialFlexTimeTotal = workStats.initialFlexTimeTotal
            ).toEntity()
        )
    }

    override suspend fun getWorkStatsByDate(date: String): SettingsState? {
        val fallback = settingsDao.loadSettings()?.toDomain()
        val workday = workdayDao.loadWorkday(date)
        return if (workday != null) {
            (fallback ?: SettingsState()).copy(
                dailyWorkTimeEstimate = workday.workTimeTodayEstimate,
                dailyLunchTimeEstimate = fallback?.dailyLunchTimeEstimate ?: ZERO_TIME,
                initialFlexTimeTotal = fallback?.initialFlexTimeTotal ?: ZERO_TIME
            )
        } else {
            fallback
        }
    }

    override suspend fun getWorkTypes(): List<String> = workTypeDao.loadWorkTypes().map { it.toDomain() }
    override suspend fun insertWorkType(workType: String) = workTypeDao.insertWorkType(workType.toWorkTypeEntity())
    override suspend fun deleteWorkType(workType: String) = workTypeDao.delete(workType.toWorkTypeEntity())
    override suspend fun clearWorkTypes() = workTypeDao.deleteAll()
}

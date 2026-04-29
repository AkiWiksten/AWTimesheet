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
        val existing = settingsDao.loadSettings()?.toDomain()
        val shouldUpdateEstimates = settings.dailyWorkTimeEstimate.isNotEmpty() ||
            settings.dailyLunchTimeEstimate != ZERO_TIME ||
            settings.initialFlexTimeTotal != ZERO_TIME

        settingsDao.insertSettings(
            SettingsState(
                name = settings.name.ifEmpty { existing?.name.orEmpty() },
                employer = settings.employer.ifEmpty { existing?.employer.orEmpty() },
                dailyWorkTimeEstimate = if (shouldUpdateEstimates) {
                    settings.dailyWorkTimeEstimate
                } else {
                    existing?.dailyWorkTimeEstimate.orEmpty()
                },
                dailyLunchTimeEstimate = if (shouldUpdateEstimates) {
                    settings.dailyLunchTimeEstimate
                } else {
                    existing?.dailyLunchTimeEstimate ?: ZERO_TIME
                },
                initialFlexTimeTotal = if (shouldUpdateEstimates) {
                    settings.initialFlexTimeTotal
                } else {
                    existing?.initialFlexTimeTotal ?: ZERO_TIME
                }
            ).toEntity()
        )
    }

    override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? {
        val fallback = settingsDao.loadSettings()?.toDomain()
        val workday = workdayDao.loadWorkday(date)
        return if (workday != null) {
            (fallback ?: SettingsState()).copy(
                dailyWorkTimeEstimate = workday.workTimeTodayEstimate
                    .ifEmpty { fallback?.dailyWorkTimeEstimate.orEmpty() },
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
    override suspend fun deleteAllWorkTypes() = workTypeDao.deleteAllWorkTypes()
}

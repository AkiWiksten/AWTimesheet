package com.akiwiksten.awtimesheet.data.repository

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.data.database.dao.CalculatedFlexTimeTotalDao
import com.akiwiksten.awtimesheet.data.database.dao.SettingsDao
import com.akiwiksten.awtimesheet.data.database.dao.WorkTypeDao
import com.akiwiksten.awtimesheet.data.database.dao.WorkdayDao
import com.akiwiksten.awtimesheet.data.mapper.toCalculatedFlextimeTotaDomain
import com.akiwiksten.awtimesheet.data.mapper.toCalculatedFlextimeTotalEntity
import com.akiwiksten.awtimesheet.data.mapper.toDomain
import com.akiwiksten.awtimesheet.data.mapper.toEntity
import com.akiwiksten.awtimesheet.data.mapper.toWorkTypeEntity
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val workdayDao: WorkdayDao,
    private val workTypeDao: WorkTypeDao,
    private val calculatedFlextimeTotalDao: CalculatedFlexTimeTotalDao
) : SettingsRepository {
    override suspend fun getSettings(): SettingsState? = settingsDao.loadSettings()?.toDomain()

    override suspend fun insertSettings(settings: SettingsState) {
        val existing = settingsDao.loadSettings()?.toDomain()
        val isStatsOnlyUpdate = settings.name.isEmpty() && settings.employer.isEmpty()
        val shouldUpdateEstimates =
            isStatsOnlyUpdate ||
                settings.dailyWorkTimeEstimate != ZERO_TIME ||
                settings.dailyLunchTimeEstimate != ZERO_TIME ||
                settings.initialFlexTimeTotal != ZERO_TIME

        settingsDao.insertSettings(
            SettingsState(
                name = settings.name.ifEmpty { existing?.name.orEmpty() },
                employer = settings.employer.ifEmpty { existing?.employer.orEmpty() },
                dailyWorkTimeEstimate = if (shouldUpdateEstimates) {
                    settings.dailyWorkTimeEstimate
                } else {
                    existing?.dailyWorkTimeEstimate ?: ZERO_TIME
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
                },
                language = settings.language.ifEmpty { existing?.language.orEmpty() },
                enableTestFeatures = settings.enableTestFeatures,
            ).toEntity()
        )
    }

    override suspend fun getCalculatedFlextimeTotal(): String =
        calculatedFlextimeTotalDao.loadCalculatedFlextimeTotal()?.toCalculatedFlextimeTotaDomain() ?: ZERO_TIME

    override suspend fun insertCalculatedFlextimeTotal(flexTime: String) {
        calculatedFlextimeTotalDao.insertCalculatedFlextimeTotal(flexTime.toCalculatedFlextimeTotalEntity())
    }

    override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? {
        val fallback = settingsDao.loadSettings()?.toDomain()
        val workday = workdayDao.loadWorkday(date)
        return if (workday != null) {
            (fallback ?: SettingsState()).copy(
                dailyWorkTimeEstimate = workday.workTimeByDateEstimate
                    .takeIf { it != ZERO_TIME && it.isNotEmpty() }
                    ?: (fallback?.dailyWorkTimeEstimate ?: ZERO_TIME),
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

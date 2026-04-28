package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.dao.SettingsDao
import com.akiwiksten.worktime30.data.database.dao.WorkdayDao
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.data.database.mapper.toSettingsState
import com.akiwiksten.worktime30.data.database.mapper.toWorkdayEntity
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayStatsRow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkdayRepositoryImpl @Inject constructor(
    private val workdayDao: WorkdayDao,
    private val settingsDao: SettingsDao
) : WorkdayRepository {
    override suspend fun loadWorkday(date: String): SettingsState? =
        workdayDao.loadWorkday(date)?.let { workday ->
            workday.toSettingsState(
                dailyLunchTimeEstimate = "",
                initialFlexTimeTotal = ""
            )
        }

    override suspend fun upsertWorkdayStats(date: String, settingsEstimates: SettingsState) {
        workdayDao.insertWorkday(settingsEstimates.toWorkdayEntity(date = date))
        val existingSettings = settingsDao.loadSettings()
        val existingGlobalStats = existingSettings?.toDomain()
        settingsDao.insertSettings(
            (existingSettings?.let {
                SettingsState(
                    name = it.name,
                    employer = it.employer,
                    dailyWorkTimeEstimate = it.dailyWorkTimeEstimate,
                    dailyLunchTimeEstimate = it.dailyLunchTimeEstimate,
                    initialFlexTimeTotal = it.initialFlexTimeTotal
                )
            } ?: SettingsState()).copy(
                dailyWorkTimeEstimate = settingsEstimates.dailyWorkTimeEstimate,
                dailyLunchTimeEstimate = settingsEstimates.dailyLunchTimeEstimate.ifEmpty {
                    existingGlobalStats?.dailyLunchTimeEstimate ?: ZERO_TIME
                },
                initialFlexTimeTotal = settingsEstimates.initialFlexTimeTotal.ifEmpty {
                    existingGlobalStats?.initialFlexTimeTotal ?: ZERO_TIME
                }
            ).toEntity()
        )
    }

    override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> =
        workdayDao.getWorkdaysByDateRange(start, end).map { row ->
            WorkdayStatsRow(
                date = row.date,
                workTimeTodayEstimate = row.workTimeTodayEstimate
            )
        }
}

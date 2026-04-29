package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.WorkdayDao
import com.akiwiksten.worktime30.data.database.mapper.toSettingsState
import com.akiwiksten.worktime30.data.database.mapper.toWorkdayEntity
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayStatsRow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkdayRepositoryImpl @Inject constructor(
    private val workdayDao: WorkdayDao
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
    }

    override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> =
        workdayDao.getWorkdaysByDateRange(start, end).map { row ->
            WorkdayStatsRow(
                date = row.date,
                workTimeByDateEstimate = row.workTimeByDateEstimate
            )
        }
}

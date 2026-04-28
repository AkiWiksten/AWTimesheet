package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.dao.WorkStatsDao
import com.akiwiksten.worktime30.data.database.dao.WorkdayDao
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.data.database.mapper.toWorkStatsState
import com.akiwiksten.worktime30.data.database.mapper.toWorkdayEntity
import com.akiwiksten.worktime30.domain.model.WorkStatsState
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayStatsRow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkdayRepositoryImpl @Inject constructor(
    private val workdayDao: WorkdayDao,
    private val workStatsDao: WorkStatsDao
) : WorkdayRepository {
    override suspend fun loadWorkday(date: String): WorkStatsState? =
        workdayDao.loadWorkday(date)?.let { workday ->
            workday.toWorkStatsState(
                dailyLunchTimeEstimate = "",
                initialFlexTimeTotal = ""
            )
        }

    override suspend fun upsertWorkdayStats(date: String, workStats: WorkStatsState) {
        workdayDao.insertWorkday(workStats.toWorkdayEntity(date = date))
        val existingGlobalStats = workStatsDao.loadWorkStats()?.let {
            WorkStatsState(
                dailyWorkTimeEstimate = it.dailyWorkTimeEstimate,
                dailyLunchTimeEstimate = it.dailyLunchTimeEstimate,
                initialFlexTimeTotal = it.initialFlexTimeTotal
            )
        }
        workStatsDao.insertWorkStats(
            WorkStatsState(
                dailyWorkTimeEstimate = workStats.dailyWorkTimeEstimate,
                dailyLunchTimeEstimate = workStats.dailyLunchTimeEstimate.ifEmpty {
                    existingGlobalStats?.dailyLunchTimeEstimate ?: ZERO_TIME
                },
                initialFlexTimeTotal = workStats.initialFlexTimeTotal.ifEmpty {
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

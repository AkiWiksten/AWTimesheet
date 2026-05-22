package com.akiwiksten.awtimesheet.domain.repository.impl

import com.akiwiksten.awtimesheet.data.database.dao.WorkdayDao
import com.akiwiksten.awtimesheet.domain.mapper.toWorkTimeByDateEstimate
import com.akiwiksten.awtimesheet.domain.mapper.toWorkdayEntity
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayStatsRow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkdayRepositoryImpl @Inject constructor(
    private val workdayDao: WorkdayDao
) : WorkdayRepository {
    override suspend fun loadWorkday(date: String): String? =
        workdayDao.loadWorkday(date)?.toWorkTimeByDateEstimate()

    override suspend fun upsertWorkdayStats(date: String, workTimeByDateEstimate: String) {
        workdayDao.insertWorkday(workTimeByDateEstimate.toWorkdayEntity(date = date))
    }

    override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> =
        workdayDao.getWorkdaysByDateRange(start, end).map { row ->
            WorkdayStatsRow(
                date = row.date,
                workTimeByDateEstimate = row.workTimeByDateEstimate
            )
        }
}

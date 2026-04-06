package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.WorkDayDao
import com.akiwiksten.worktime30.data.database.dao.WorkStatsDao
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkDayRepositoryImpl @Inject constructor(
    private val workDayDao: WorkDayDao,
    private val workStatsDao: WorkStatsDao
) : WorkDayRepository {
    override suspend fun getWorkDay(date: String, projectName: String): WorkDayEntity? =
        workDayDao.loadWorkDay(date, projectName)

    override suspend fun insertWorkDay(workDay: WorkDayEntity) = workDayDao.insertWorkDay(workDay)
    override suspend fun getWorkStats(): WorkStatsEntity? = workStatsDao.loadWorkStats()
    override suspend fun insertWorkStats(workStats: WorkStatsEntity) =
        workStatsDao.insertWorkStats(workStats)

    override suspend fun getWorkDaysByDateRange(start: String, end: String): List<WorkDayEntity> =
        workDayDao.getWorkDaysByDateRange(start, end)
}

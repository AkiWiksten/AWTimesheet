package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.WorkdayDao
import com.akiwiksten.worktime30.data.database.dao.WorkStatsDao
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkdayRepositoryImpl @Inject constructor(
    private val workdayDao: WorkdayDao,
    private val workStatsDao: WorkStatsDao
) : WorkdayRepository {
    override suspend fun getWorkday(date: String, projectName: String): WorkdayEntity? =
        workdayDao.loadWorkday(date, projectName)

    override suspend fun insertWorkday(workday: WorkdayEntity) = workdayDao.insertWorkday(workday)

    override suspend fun deleteWorkday(workday: WorkdayEntity) = workdayDao.delete(workday)

    override suspend fun getWorkStats(): WorkStatsEntity? = workStatsDao.loadWorkStats()
    override suspend fun insertWorkStats(workStats: WorkStatsEntity) =
        workStatsDao.insertWorkStats(workStats)

    override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayEntity> =
        workdayDao.getWorkdaysByDateRange(start, end)
}

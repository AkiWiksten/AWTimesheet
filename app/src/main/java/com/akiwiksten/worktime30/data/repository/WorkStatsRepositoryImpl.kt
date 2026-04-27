package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.dao.WorkStatsDao
import com.akiwiksten.worktime30.data.database.dao.WorkdayDao
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.data.database.mapper.toWorkStatsState
import com.akiwiksten.worktime30.domain.model.WorkStatsState
import com.akiwiksten.worktime30.domain.repository.WorkStatsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkStatsRepositoryImpl @Inject constructor(
    private val workStatsDao: WorkStatsDao,
    private val workdayDao: WorkdayDao
) : WorkStatsRepository {
    override suspend fun getWorkStats(): WorkStatsState? =
        workStatsDao.loadWorkStats()?.toDomain()

    override suspend fun insertWorkStats(workStats: WorkStatsState) =
        workStatsDao.insertWorkStats(workStats.toEntity())

    override suspend fun getWorkStatsByDate(date: String): WorkStatsState? {
        val fallback = workStatsDao.loadWorkStats()?.toDomain()
        val workday = workdayDao.loadWorkday(date)

        return if (workday != null) {
            workday.toWorkStatsState(
                dailyLunchTimeEstimate = fallback?.dailyLunchTimeEstimate ?: ZERO_TIME,
                initialFlexTimeTotal = fallback?.initialFlexTimeTotal ?: ZERO_TIME
            )
        } else {
            fallback
        }
    }
}

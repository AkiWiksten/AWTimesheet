package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.dao.SettingsDao
import com.akiwiksten.worktime30.data.database.dao.WorkdayDao
import com.akiwiksten.worktime30.data.database.mapper.mergeIntoSettings
import com.akiwiksten.worktime30.data.database.mapper.toWorkStatsDomain
import com.akiwiksten.worktime30.data.database.mapper.toWorkStatsState
import com.akiwiksten.worktime30.domain.model.WorkStatsState
import com.akiwiksten.worktime30.domain.repository.WorkStatsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkStatsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
    private val workdayDao: WorkdayDao
) : WorkStatsRepository {
    override suspend fun getWorkStats(): WorkStatsState? =
        settingsDao.loadSettings()?.toWorkStatsDomain()

    override suspend fun insertWorkStats(workStats: WorkStatsState) {
        val existing = settingsDao.loadSettings()
        settingsDao.insertSettings(workStats.mergeIntoSettings(existing))
    }

    override suspend fun getWorkStatsByDate(date: String): WorkStatsState? {
        val fallback = settingsDao.loadSettings()?.toWorkStatsDomain()
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

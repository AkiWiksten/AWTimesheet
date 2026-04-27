@file:Suppress("ImportOrdering")

package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.dao.ProjectDetailsDao
import com.akiwiksten.worktime30.data.database.dao.WorkdayDao
import com.akiwiksten.worktime30.data.database.dao.WorkStatsDao
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.data.database.mapper.toWorkdayEntity
import com.akiwiksten.worktime30.data.database.mapper.toWorkStatsState
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectDetailsRepositoryImpl @Inject constructor(
    private val projectDetailsDao: ProjectDetailsDao,
    private val workStatsDao: WorkStatsDao,
    private val workdayDao: WorkdayDao
) : ProjectDetailsRepository {
    override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? =
        projectDetailsDao.loadProjectDetails(date, projectName)?.toDomain()

    override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) =
        projectDetailsDao.insertProjectDetails(projectDetails.toEntity())

    override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) =
        projectDetailsDao.delete(projectDetails.toEntity())

    override suspend fun getWorkStats(): WorkStatsState? = workStatsDao.loadWorkStats()?.toDomain()
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

    override suspend fun upsertWorkdayStats(date: String, workTimeToday: String, workStats: WorkStatsState) {
        workdayDao.insertWorkday(workStats.toWorkdayEntity(date = date, workTimeToday = workTimeToday))
        val existingGlobalStats = workStatsDao.loadWorkStats()?.toDomain()
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

    override suspend fun getWorkdayStatsByDateRange(start: String, end: String): List<WorkdayStatsRow> {
        return workdayDao.getWorkdaysByDateRange(start, end).map { row ->
            WorkdayStatsRow(
                date = row.date,
                workTimeToday = row.workTimeToday,
                workTimeTodayEstimate = row.workTimeTodayEstimate
            )
        }
    }

    override suspend fun getProjectDetailsByDateRange(start: String, end: String): List<ProjectDetailsState> =
        projectDetailsDao.getProjectDetailsByDateRange(start, end).map { it.toDomain() }
}

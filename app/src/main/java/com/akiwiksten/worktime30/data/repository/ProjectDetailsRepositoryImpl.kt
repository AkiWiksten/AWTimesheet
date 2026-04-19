package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.ProjectDetailsDao
import com.akiwiksten.worktime30.data.database.dao.WorkStatsDao
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.feature.projects.single.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.single.details.WorkStatsState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectDetailsRepositoryImpl @Inject constructor(
    private val projectDetailsDao: ProjectDetailsDao,
    private val workStatsDao: WorkStatsDao
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

    override suspend fun getProjectDetailsByDateRange(start: String, end: String): List<ProjectDetailsState> =
        projectDetailsDao.getProjectDetailsByDateRange(start, end).map { it.toDomain() }
}

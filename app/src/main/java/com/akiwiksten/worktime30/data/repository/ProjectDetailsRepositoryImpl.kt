package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.ProjectDetailsDao
import com.akiwiksten.worktime30.data.database.dao.WorkStatsDao
import com.akiwiksten.worktime30.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.feature.projects.single.details.ProjectDetailsState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectDetailsRepositoryImpl @Inject constructor(
    private val projectDetailsDao: ProjectDetailsDao,
    private val workStatsDao: WorkStatsDao
) : ProjectDetailsRepository {
    override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? =
        projectDetailsDao.loadProjectDetails(date, projectName)?.toDomain()

    override suspend fun insertProjectDetails(projectDetails: ProjectDetailsEntity) =
        projectDetailsDao.insertProjectDetails(projectDetails)

    override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsEntity) =
        projectDetailsDao.delete(projectDetails)

    override suspend fun getWorkStats(): WorkStatsEntity? = workStatsDao.loadWorkStats()
    override suspend fun insertWorkStats(workStats: WorkStatsEntity) =
        workStatsDao.insertWorkStats(workStats)

    override suspend fun getProjectDetailsByDateRange(start: String, end: String): List<ProjectDetailsEntity> =
        projectDetailsDao.getProjectDetailsByDateRange(start, end)
}

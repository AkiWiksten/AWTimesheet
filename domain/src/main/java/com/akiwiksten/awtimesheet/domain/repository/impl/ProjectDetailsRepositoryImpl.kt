package com.akiwiksten.awtimesheet.domain.repository.impl

import com.akiwiksten.awtimesheet.data.database.dao.ProjectDetailsDao
import com.akiwiksten.awtimesheet.domain.mapper.toDomain
import com.akiwiksten.awtimesheet.domain.mapper.toEntity
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.repository.ProjectDetailsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectDetailsRepositoryImpl @Inject constructor(
    private val projectDetailsDao: ProjectDetailsDao
) : ProjectDetailsRepository {
    override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? =
        projectDetailsDao.loadProjectDetails(date, projectName)?.toDomain()

    override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) =
        projectDetailsDao.insertProjectDetails(projectDetails.toEntity())

    override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) =
        projectDetailsDao.delete(projectDetails.toEntity())

    override suspend fun getProjectDetailsByDateRange(start: String, end: String): List<ProjectDetailsState> =
        projectDetailsDao.getProjectDetailsByDateRange(start, end).map { it.toDomain() }
}
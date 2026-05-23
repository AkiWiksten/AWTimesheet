package com.akiwiksten.awtimesheet.data.repository

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.data.database.dao.ProjectDao
import com.akiwiksten.awtimesheet.data.database.dao.ProjectNameDao
import com.akiwiksten.awtimesheet.data.mapper.toDomain
import com.akiwiksten.awtimesheet.data.mapper.toEntity
import com.akiwiksten.awtimesheet.data.mapper.toProjectNameEntity
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val projectNameDao: ProjectNameDao
) : ProjectRepository {
    override suspend fun anyRecords(): Boolean = projectNameDao.anyRecords()

    override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> =
        projectDao.getProjectsByDateRange(start, end).map { it.toDomain() }

    override suspend fun getProject(date: String, projectName: String): SingleProjectState? =
        projectDao.loadProject(date, projectName)?.toDomain()

    override suspend fun insertProject(project: SingleProjectState) = projectDao.insertProject(project.toEntity())

    override suspend fun deleteProject(project: SingleProjectState) = projectDao.delete(project.toEntity())

    override suspend fun getProjectNames(): List<String> = projectNameDao
        .loadProjectNames()
        .map { it.toDomain() }

    override suspend fun insertProjectName(projectName: String) =
        projectNameDao.insertProjectName(projectName.toProjectNameEntity())

    override suspend fun deleteProjectName(projectName: String) =
        projectNameDao.delete(projectName.toProjectNameEntity())

    override suspend fun isProjectNameUsed(projectName: String): Boolean =
        projectDao.isProjectNameUsed(projectName)

    override suspend fun getWorkTimeByDate(date: String): String =
        projectDao.getProjectTimesByDate(date).fold(ZERO_TIME) { acc, time ->
            WorkTimeCalculator.calculateFlexTime(acc, time)
        }
}


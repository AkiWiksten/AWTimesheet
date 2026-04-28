package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.ProjectDao
import com.akiwiksten.worktime30.data.database.dao.ProjectNameDao
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.data.database.mapper.toProjectNameEntity
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val projectNameDao: ProjectNameDao
) : ProjectRepository {
    override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> =
        projectDao.getProjectsByDateRange(start, end).map { it.toDomain() }

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

    override suspend fun getProjectTimeSumByDate(date: String): String =
        projectDao.getProjectTimesByDate(date).fold(ZERO_TIME) { acc, time ->
            WorkTimeCalculator.calculateFlexTime(acc, time)
        }
}

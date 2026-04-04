package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.ProjectDao
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.dao.ProjectNameDao
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val projectNameDao: ProjectNameDao
) : ProjectRepository {
    override suspend fun getProjectsByDateRange(start: String, end: String): List<ProjectEntity> =
        projectDao.getProjectsByDateRange(start, end)

    override suspend fun insertProject(project: ProjectEntity) = projectDao.insertProject(project)

    override suspend fun deleteProject(project: ProjectEntity) = projectDao.delete(project)

    override suspend fun getProjectNames(): List<ProjectNameEntity> = projectNameDao.loadProjectNames()

    override suspend fun insertProjectName(projectName: ProjectNameEntity) =
        projectNameDao.insertProjectName(projectName)

    override suspend fun deleteProjectName(projectName: ProjectNameEntity) =
        projectNameDao.delete(projectName)
}
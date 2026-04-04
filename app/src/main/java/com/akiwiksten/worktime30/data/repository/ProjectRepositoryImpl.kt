package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.Project
import com.akiwiksten.worktime30.data.database.ProjectDao
import com.akiwiksten.worktime30.data.database.ProjectName
import com.akiwiksten.worktime30.data.database.ProjectNameDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val projectDao: ProjectDao,
    private val projectNameDao: ProjectNameDao
) : ProjectRepository {
    override suspend fun getProjectsByDateRange(start: String, end: String): List<Project> =
        projectDao.getProjectsByDateRange(start, end)

    override suspend fun insertProject(project: Project) = projectDao.insertProject(project)

    override suspend fun deleteProject(project: Project) = projectDao.delete(project)

    override suspend fun getProjectNames(): List<ProjectName> = projectNameDao.loadProjectNames()

    override suspend fun insertProjectName(projectName: ProjectName) =
        projectNameDao.insertProjectName(projectName)

    override suspend fun deleteProjectName(projectName: ProjectName) =
        projectNameDao.delete(projectName)
}
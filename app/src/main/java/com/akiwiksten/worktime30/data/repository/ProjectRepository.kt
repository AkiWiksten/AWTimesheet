package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.Project
import com.akiwiksten.worktime30.data.database.ProjectName

interface ProjectRepository {
    suspend fun getProjectsByDateRange(start: String, end: String): List<Project>
    suspend fun insertProject(project: Project)
    suspend fun deleteProject(project: Project)
    suspend fun getProjectNames(): List<ProjectName>
    suspend fun insertProjectName(projectName: ProjectName)
    suspend fun deleteProjectName(projectName: ProjectName)
}

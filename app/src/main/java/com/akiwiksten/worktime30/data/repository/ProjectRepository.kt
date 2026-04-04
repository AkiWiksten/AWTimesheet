package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity

interface ProjectRepository {
    suspend fun getProjectsByDateRange(start: String, end: String): List<ProjectEntity>
    suspend fun insertProject(project: ProjectEntity)
    suspend fun deleteProject(project: ProjectEntity)
    suspend fun getProjectNames(): List<ProjectNameEntity>
    suspend fun insertProjectName(projectName: ProjectNameEntity)
    suspend fun deleteProjectName(projectName: ProjectNameEntity)
}

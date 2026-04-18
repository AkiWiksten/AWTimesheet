package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState

interface ProjectRepository {
    suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState>
    suspend fun insertProject(project: SingleProjectState)
    suspend fun deleteProject(project: ProjectEntity)
    suspend fun getProjectNames(): List<String>
    suspend fun insertProjectName(projectName: ProjectNameEntity)
    suspend fun deleteProjectName(projectName: ProjectNameEntity)
    suspend fun isProjectNameUsed(projectName: String): Boolean
}

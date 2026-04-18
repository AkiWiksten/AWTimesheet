package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState

interface ProjectRepository {
    suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState>
    suspend fun insertProject(project: SingleProjectState)
    suspend fun deleteProject(project: SingleProjectState)
    suspend fun getProjectNames(): List<String>
    suspend fun insertProjectName(projectName: String)
    suspend fun deleteProjectName(projectName: String)
    suspend fun isProjectNameUsed(projectName: String): Boolean
}

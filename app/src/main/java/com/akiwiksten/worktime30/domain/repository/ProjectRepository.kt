package com.akiwiksten.worktime30.domain.repository

import com.akiwiksten.worktime30.feature.workday.SingleProjectState

interface ProjectRepository {
    suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState>
    suspend fun insertProject(project: SingleProjectState)
    suspend fun deleteProject(project: SingleProjectState)
    suspend fun getProjectNames(): List<String>
    suspend fun insertProjectName(projectName: String)
    suspend fun deleteProjectName(projectName: String)
    suspend fun isProjectNameUsed(projectName: String): Boolean
}

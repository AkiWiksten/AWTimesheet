package com.akiwiksten.worktime30.domain.repository

import com.akiwiksten.worktime30.domain.model.SingleProjectState

interface ProjectRepository {
    suspend fun anyRecords(): Boolean
    suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState>
    suspend fun getProject(date: String, projectName: String): SingleProjectState?
    suspend fun insertProject(project: SingleProjectState)
    suspend fun deleteProject(project: SingleProjectState)
    suspend fun getProjectNames(): List<String>
    suspend fun insertProjectName(projectName: String)
    suspend fun deleteProjectName(projectName: String)
    suspend fun isProjectNameUsed(projectName: String): Boolean
    suspend fun getWorkTimeByDate(date: String): String
}

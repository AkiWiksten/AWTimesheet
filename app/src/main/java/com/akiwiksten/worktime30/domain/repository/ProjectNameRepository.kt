package com.akiwiksten.worktime30.domain.repository

interface ProjectNameRepository {
    suspend fun insertProjectName(projectName: String)
    suspend fun loadProjectNames(): List<String>
    suspend fun deleteProjectName(projectName: String)
}

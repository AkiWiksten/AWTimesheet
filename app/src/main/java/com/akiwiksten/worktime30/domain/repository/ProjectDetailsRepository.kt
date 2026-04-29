package com.akiwiksten.worktime30.domain.repository

import com.akiwiksten.worktime30.domain.model.ProjectDetailsState

interface ProjectDetailsRepository {
    suspend fun getProjectDetails(date: String, projectName: String = ""): ProjectDetailsState?
    suspend fun insertProjectDetails(projectDetails: ProjectDetailsState)
    suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState)
    suspend fun getProjectDetailsByDateRange(start: String, end: String): List<ProjectDetailsState>
}

data class WorkdayStatsRow(
    val date: String,
    val workTimeByDateEstimate: String
)

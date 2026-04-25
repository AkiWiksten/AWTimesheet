package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState

interface ProjectDetailsRepository {
    suspend fun getProjectDetails(date: String, projectName: String = ""): ProjectDetailsState?
    suspend fun insertProjectDetails(projectDetails: ProjectDetailsState)
    suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState)
    suspend fun getWorkStats(): WorkStatsState?
    suspend fun insertWorkStats(workStats: WorkStatsState)
    suspend fun getWorkStatsByDate(date: String): WorkStatsState? = getWorkStats()
    suspend fun upsertWorkdayStats(date: String, workTimeToday: String, workStats: WorkStatsState) {
        insertWorkStats(workStats)
    }
    suspend fun getProjectDetailsByDateRange(start: String, end: String): List<ProjectDetailsState>
}

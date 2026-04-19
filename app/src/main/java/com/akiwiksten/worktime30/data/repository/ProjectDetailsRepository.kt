package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.worktime30.feature.projects.single.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.single.details.WorkStatsState

interface ProjectDetailsRepository {
    suspend fun getProjectDetails(date: String, projectName: String = ""): ProjectDetailsState?
    suspend fun insertProjectDetails(projectDetails: ProjectDetailsEntity)
    suspend fun deleteProjectDetails(projectDetails: ProjectDetailsEntity)
    suspend fun getWorkStats(): WorkStatsState?
    suspend fun insertWorkStats(workStats: WorkStatsState)
    suspend fun getProjectDetailsByDateRange(start: String, end: String): List<ProjectDetailsEntity>
}

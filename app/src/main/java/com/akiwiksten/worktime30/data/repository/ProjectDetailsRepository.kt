package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.feature.projects.single.details.ProjectDetailsState

interface ProjectDetailsRepository {
    suspend fun getProjectDetails(date: String, projectName: String = ""): ProjectDetailsState?
    suspend fun insertProjectDetails(projectDetails: ProjectDetailsEntity)
    suspend fun deleteProjectDetails(projectDetails: ProjectDetailsEntity)
    suspend fun getWorkStats(): WorkStatsEntity?
    suspend fun insertWorkStats(workStats: WorkStatsEntity)
    suspend fun getProjectDetailsByDateRange(start: String, end: String): List<ProjectDetailsEntity>
}

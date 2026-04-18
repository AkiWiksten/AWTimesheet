package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import javax.inject.Inject

class GetProjectsScreenDataUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(date: String): ProjectsScreenData {
        val projects = projectRepository.getProjectsByDateRange(date, date)
        val projectTime = projects.fold(ZERO_TIME) { acc, project ->
            WorkTimeCalculator.calculateWorkTimeBalance(acc, project.projectTime)
        }

        val projectNames = projectRepository.getProjectNames()

        val workTypes = settingsRepository.getWorkTypes().map { it.workType }

        return ProjectsScreenData(
            projectTime = projectTime,
            projects = projects,
            projectNames = projectNames,
            workTypes = workTypes
        )
    }
}

data class ProjectsScreenData(
    val projectTime: String,
    val projects: List<ProjectEntity>,
    val projectNames: List<ProjectNameEntity>,
    val workTypes: List<String>
)

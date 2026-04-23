package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.feature.workday.SingleProjectState
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import javax.inject.Inject

class DeleteWorkdayUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectDetailsRepository: ProjectDetailsRepository
) {
    suspend operator fun invoke(date: String, projectName: String, projectTime: String = ZERO_TIME) {
        projectRepository.deleteProject(
            SingleProjectState(date = date, projectName = projectName, projectTime = ZERO_TIME)
        )
        projectDetailsRepository.deleteProjectDetails(
            ProjectDetailsState(date = date, projectName = projectName)
        )

        val shouldDeleteProjectName = projectTime == ZERO_TIME || !projectRepository.isProjectNameUsed(projectName)
        if (shouldDeleteProjectName) {
            projectRepository.deleteProjectName(projectName)
        }
    }
}


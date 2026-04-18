package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState
import javax.inject.Inject

class DeleteProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectDetailsRepository: ProjectDetailsRepository
) {
    suspend operator fun invoke(date: String, projectName: String) {
        projectRepository.deleteProject(
            SingleProjectState(date = date, projectName = projectName, projectTime = ZERO_TIME)
        )
        projectDetailsRepository.deleteProjectDetails(ProjectDetailsEntity(date = date, projectName = projectName))

        if (!projectRepository.isProjectNameUsed(projectName)) {
            projectRepository.deleteProjectName(projectName)
        }
    }
}

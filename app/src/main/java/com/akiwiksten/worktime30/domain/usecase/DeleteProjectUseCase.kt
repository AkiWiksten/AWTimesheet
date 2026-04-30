package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import javax.inject.Inject

class DeleteProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectDetailsRepository: ProjectDetailsRepository
) {
    suspend operator fun invoke(date: String, projectName: String, projectTime: String = ZERO_TIME) {
        if (projectTime == ZERO_TIME) {
            projectRepository.deleteProjectName(projectName)
            return
        }

        projectRepository.deleteProject(
            SingleProjectState(date = date, projectName = projectName, projectTime = projectTime)
        )
        projectDetailsRepository.deleteProjectDetails(
            ProjectDetailsState(date = date, projectName = projectName)
        )

        if (!projectRepository.isProjectNameUsed(projectName)) {
            projectRepository.deleteProjectName(projectName)
        }
    }
}

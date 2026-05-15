package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectDetailsRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
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
    }
}

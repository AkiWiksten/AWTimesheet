package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectDetailsRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import javax.inject.Inject

class DeleteDraftProjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectDetailsRepository: ProjectDetailsRepository
) {
    suspend operator fun invoke(date: String, projectName: String, projectTime: String = ZERO_TIME) {
        projectRepository.deleteProjectName(projectName)

        val projectToDelete = projectRepository.getProject(
            date = date,
            projectName = projectName
        )
        if (projectToDelete?.isDraft == true) {
            projectRepository.deleteProject(projectToDelete)
            projectDetailsRepository.deleteProjectDetails(
                ProjectDetailsState(
                    date = date,
                    projectName = projectName
                )
            )
        }
    }
}

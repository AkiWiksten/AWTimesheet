package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.model.isProjectNameOnlyPlaceholder
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

        deleteRecordedProject(
            SingleProjectState(date = date, projectName = projectName, projectTime = projectTime)
        )
    }

    suspend operator fun invoke(date: String, project: SingleProjectState) {
        if (project.isProjectNameOnlyPlaceholder()) {
            projectRepository.deleteProjectName(project.projectName)
            return
        }

        deleteRecordedProject(project.copy(date = project.date.ifBlank { date }))
    }

    private suspend fun deleteRecordedProject(project: SingleProjectState) {
        projectRepository.deleteProject(
            project
        )
        projectDetailsRepository.deleteProjectDetails(
            ProjectDetailsState(date = project.date, projectName = project.projectName)
        )
    }
}

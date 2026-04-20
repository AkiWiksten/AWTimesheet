package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import javax.inject.Inject

class SaveProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectDetailsRepository: ProjectDetailsRepository
) {
    suspend operator fun invoke(
        projectsToSave: List<SingleProjectState>,
        projectDetailsToSave: ProjectDetailsState? = null
    ) {
        projectsToSave.forEach { project ->
            projectRepository.insertProjectName(project.projectName)
            projectRepository.insertProject(project)
        }

        projectDetailsToSave?.let {
            projectDetailsRepository.insertProjectDetails(it)
        }
    }
}

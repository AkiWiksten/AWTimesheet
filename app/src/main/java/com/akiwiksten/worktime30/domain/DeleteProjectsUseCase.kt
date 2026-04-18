package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import javax.inject.Inject

class DeleteProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectDetailsRepository: ProjectDetailsRepository
) {
    suspend operator fun invoke(date: String, projectName: String) {
        projectRepository.deleteProject(
            ProjectEntity(date = date, projectName = projectName, projectTime = ZERO_TIME)
        )
        projectDetailsRepository.deleteProjectDetails(ProjectDetailsEntity(date = date, projectName = projectName))

        if (!projectRepository.isProjectNameUsed(projectName)) {
            projectRepository.deleteProjectName(ProjectNameEntity(projectName))
        }
    }
}

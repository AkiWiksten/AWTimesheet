package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import javax.inject.Inject

class SaveProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(
        date: String,
        projectsToSave: List<ProjectEntity>,
        projectNamesToDelete: List<String>
    ) {
        projectsToSave.forEach { project ->
            projectRepository.insertProject(project)
            projectRepository.insertProjectName(ProjectNameEntity(project.projectName))
        }
        projectNamesToDelete.forEach { name ->
            projectRepository.deleteProject(
                ProjectEntity(date = date, projectName = name, projectTime = ZERO_TIME)
            )
            projectRepository.deleteProjectName(ProjectNameEntity(name))
        }
    }
}

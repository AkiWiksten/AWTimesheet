package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.WorkdayRepository
import javax.inject.Inject

class DeleteProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val workdayRepository: WorkdayRepository
) {
    suspend operator fun invoke(date: String, projectName: String) {
        projectRepository.deleteProject(
            ProjectEntity(date = date, projectName = projectName, projectTime = ZERO_TIME)
        )
        workdayRepository.deleteWorkday(WorkdayEntity(date = date, projectName = projectName))

        if (!projectRepository.isProjectNameUsed(projectName)) {
            projectRepository.deleteProjectName(ProjectNameEntity(projectName))
        }
    }
}

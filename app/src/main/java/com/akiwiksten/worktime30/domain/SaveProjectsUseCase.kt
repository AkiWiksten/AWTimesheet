package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.WorkdayRepository
import javax.inject.Inject

class SaveProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val workdayRepository: WorkdayRepository
) {
    suspend operator fun invoke(
        date: String,
        projectsToSave: List<ProjectEntity>,
        projectNamesToDelete: List<String>,
        workdayToSave: WorkdayEntity? = null
    ) {
        projectsToSave.forEach { project ->
            projectRepository.insertProjectName(ProjectNameEntity(project.projectName))
            projectRepository.insertProject(project)
        }
        
        workdayToSave?.let {
            workdayRepository.insertWorkday(it)
        }
        
        projectNamesToDelete.forEach { name ->
            projectRepository.deleteProject(
                ProjectEntity(date = date, projectName = name, projectTime = ZERO_TIME)
            )
            workdayRepository.deleteWorkday(WorkdayEntity(date = date, projectName = name))
            
            if (!projectRepository.isProjectNameUsed(name)) {
                projectRepository.deleteProjectName(ProjectNameEntity(name))
            }
        }
    }
}

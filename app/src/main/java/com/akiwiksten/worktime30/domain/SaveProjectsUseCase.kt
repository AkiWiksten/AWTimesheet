package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.WorkDayRepository
import javax.inject.Inject

class SaveProjectsUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val workDayRepository: WorkDayRepository
) {
    suspend operator fun invoke(
        date: String,
        projectsToSave: List<ProjectEntity>,
        projectNamesToDelete: List<String>,
        workDayToSave: WorkDayEntity? = null
    ) {
        projectsToSave.forEach { project ->
            projectRepository.insertProjectName(ProjectNameEntity(project.projectName))
            projectRepository.insertProject(project)
        }
        
        workDayToSave?.let {
            workDayRepository.insertWorkDay(it)
        }
        
        projectNamesToDelete.forEach { name ->
            projectRepository.deleteProject(
                ProjectEntity(date = date, projectName = name, projectTime = ZERO_TIME)
            )
            workDayRepository.deleteWorkDay(WorkDayEntity(date = date, projectName = name))
            
            if (!projectRepository.isProjectNameUsed(name)) {
                projectRepository.deleteProjectName(ProjectNameEntity(name))
            }
        }
    }
}

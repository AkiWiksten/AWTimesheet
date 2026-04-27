package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import javax.inject.Inject

class DeleteWorkdayUseCase @Inject constructor(
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

        val recalculatedWorkTimeToday = projectRepository
            .getProjectsByDateRange(date, date)
            .fold(ZERO_TIME) { acc, project ->
                WorkTimeCalculator.calculateFlexTime(acc, project.projectTime)
            }
        val currentWorkStats = projectDetailsRepository.getWorkStatsByDate(date)
        if (currentWorkStats != null) {
            projectDetailsRepository.upsertWorkdayStats(
                date = date,
                workTimeToday = recalculatedWorkTimeToday,
                workStats = currentWorkStats
            )
        }

        if (!projectRepository.isProjectNameUsed(projectName)) {
            projectRepository.deleteProjectName(projectName)
        }
    }
}

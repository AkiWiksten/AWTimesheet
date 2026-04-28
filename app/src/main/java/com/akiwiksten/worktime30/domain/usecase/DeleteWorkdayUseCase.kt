package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.repository.WorkStatsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import javax.inject.Inject

class DeleteWorkdayUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectDetailsRepository: ProjectDetailsRepository,
    private val workStatsRepository: WorkStatsRepository,
    private val workdayRepository: WorkdayRepository
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

        val recalculatedWorkTimeToday = projectRepository.getProjectTimeSumByDate(date)
        val currentWorkStats = workStatsRepository.getWorkStatsByDate(date)
        if (currentWorkStats != null) {
            workdayRepository.upsertWorkdayStats(
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

package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import com.akiwiksten.worktime30.feature.workday.SingleProjectState
import javax.inject.Inject

class SaveWorkdayUseCase @Inject constructor(
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

        // Keep WorkdayEntity rows in sync for new/updated project data.
        val affectedDates = projectsToSave.map { it.date }.filter { it.isNotEmpty() }.distinct()
        affectedDates.forEach { date ->
            val workTimeToday = projectRepository
                .getProjectsByDateRange(date, date)
                .fold(ZERO_TIME) { acc, project ->
                    WorkTimeCalculator.calculateFlexTime(acc, project.projectTime)
                }

            val existing = projectDetailsRepository.getWorkStatsByDate(date)
                ?: projectDetailsRepository.getWorkStats()
                ?: WorkStatsState(
                    dailyWorkTimeEstimate = DEFAULT_DAILY_WORK_TIME,
                    dailyLunchTimeEstimate = ZERO_TIME,
                    initialFlexTimeTotal = ZERO_TIME
                )

            projectDetailsRepository.upsertWorkdayStats(
                date = date,
                workTimeToday = workTimeToday,
                workStats = existing
            )
        }
    }
}

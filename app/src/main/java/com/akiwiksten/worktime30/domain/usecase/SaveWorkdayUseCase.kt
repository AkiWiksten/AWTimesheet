package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.model.WorkStatsState
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.repository.WorkStatsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import javax.inject.Inject

class SaveWorkdayUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectDetailsRepository: ProjectDetailsRepository,
    private val workStatsRepository: WorkStatsRepository,
    private val workdayRepository: WorkdayRepository
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

        // Ensure each affected date has an estimate row for flex-time aggregation.
        val affectedDates = projectsToSave.map { it.date }.filter { it.isNotEmpty() }.distinct()
        affectedDates.forEach { date ->
            val existing = workStatsRepository.getWorkStatsByDate(date)
                ?: workStatsRepository.getWorkStats()
                ?: WorkStatsState(
                    dailyWorkTimeEstimate = DEFAULT_DAILY_WORK_TIME,
                    dailyLunchTimeEstimate = ZERO_TIME,
                    initialFlexTimeTotal = ZERO_TIME
                )
            workdayRepository.upsertWorkdayStats(date = date, workStats = existing)
        }
    }
}

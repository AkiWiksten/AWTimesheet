package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import javax.inject.Inject

class SaveWorkdayUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectDetailsRepository: ProjectDetailsRepository,
    private val settingsRepository: SettingsRepository,
    private val workdayRepository: WorkdayRepository
) {
    suspend operator fun invoke(
        projectsToSave: List<SingleProjectState>,
        projectDetailsToSave: ProjectDetailsState? = null
    ) {
        projectsToSave.forEach { project ->
            projectRepository.insertProjectName(project.projectName)
            projectRepository.insertProject(project)
            if(projectDetailsToSave == null) {
                projectDetailsRepository
                    .insertProjectDetails(ProjectDetailsState()
                        .copy(projectTime = project.projectTime))
            }
        }

        projectDetailsToSave?.let {
            projectDetailsRepository.insertProjectDetails(it)
        }



        // Ensure each affected date has an estimate row for flex-time aggregation.
        val affectedDates = projectsToSave.map { it.date }.filter { it.isNotEmpty() }.distinct()
        affectedDates.forEach { date ->
            val existing = settingsRepository.getEffectiveSettingsForDate(date)
                ?: settingsRepository.getSettings()
                ?: SettingsState(
                    dailyWorkTimeEstimate = DEFAULT_DAILY_WORK_TIME,
                    dailyLunchTimeEstimate = ZERO_TIME,
                    initialFlexTimeTotal = ZERO_TIME
                )
            workdayRepository.upsertWorkdayStats(
                date = date,
                workTimeByDateEstimate = existing.dailyWorkTimeEstimate
            )
        }
    }
}

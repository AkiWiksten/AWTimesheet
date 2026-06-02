package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectDetailsRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import javax.inject.Inject

class SaveWorkdayUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val projectDetailsRepository: ProjectDetailsRepository,
    private val settingsRepository: SettingsRepository,
    private val workdayRepository: WorkdayRepository
) {
    suspend operator fun invoke(
        projectToSave: SingleProjectState,
        projectDetailsToSave: ProjectDetailsState? = null
    ) {
        projectRepository.insertProjectName(projectToSave.projectName)
        projectRepository.insertProject(projectToSave)

        projectDetailsToSave?.let {
            projectDetailsRepository.insertProjectDetails(it)
        }

        if (projectToSave.date.isNotEmpty()) {
            val existing = settingsRepository.getEffectiveSettingsForDate(projectToSave.date)
                ?: settingsRepository.getSettings()
                ?: SettingsState(
                    dailyWorkTimeEstimate = DEFAULT_DAILY_WORK_TIME,
                    dailyLunchTimeEstimate = ZERO_TIME,
                    initialFlexTimeTotal = ZERO_TIME
                )
            workdayRepository.upsertWorkdayStats(
                date = projectToSave.date,
                workTimeByDateEstimate = existing.dailyWorkTimeEstimate
            )
        }
    }
}

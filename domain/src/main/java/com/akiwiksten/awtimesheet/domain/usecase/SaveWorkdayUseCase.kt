package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.AbsenceFlexDayMatcher
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
        projectDetailsToSave: ProjectDetailsState? = null,
        localizedFlexDayWorkType: String = "",
        originalProjectName: String = ""
    ) {
        if (isAbsenceFlexDay(projectToSave, localizedFlexDayWorkType)) {
            clearOtherProjectsForDate(projectToSave)
        }

        if (originalProjectName.isNotBlank() && originalProjectName != projectToSave.projectName) {
            val oldProject = projectRepository.getProject(projectToSave.date, originalProjectName)
            if (oldProject != null) {
                projectRepository.deleteProject(oldProject)
            }
            val oldDetails = projectDetailsRepository.getProjectDetails(projectToSave.date, originalProjectName)
            if (oldDetails != null) {
                projectDetailsRepository.deleteProjectDetails(oldDetails)
            }
        }

        projectRepository.insertProjectName(projectToSave.projectName)
        projectRepository.insertProject(projectToSave)
        if (projectDetailsToSave != null) projectDetailsRepository.insertProjectDetails(projectDetailsToSave)

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

    private suspend fun clearOtherProjectsForDate(projectToSave: SingleProjectState) {
        if (projectToSave.date.isBlank()) return

        val sameDayProjects = projectRepository.getProjectsByDateRange(
            start = projectToSave.date,
            end = projectToSave.date
        )

        sameDayProjects
            .filterNot { it.projectName == projectToSave.projectName }
            .forEach { project ->
                projectRepository.deleteProject(project)
            }
    }

    private fun isAbsenceFlexDay(project: SingleProjectState, localizedFlexDayWorkType: String): Boolean {
        return AbsenceFlexDayMatcher.isAbsenceFlexDay(
            workType = project.workType,
            projectName = project.projectName,
            localizedFlexDayWorkType = localizedFlexDayWorkType
        )
    }
}

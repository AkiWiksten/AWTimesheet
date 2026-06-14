package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import javax.inject.Inject

class GetWorkdayScreenDataUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend operator fun invoke(date: String): WorkdayScreenData {
        val projects = projectRepository.getProjectsByDateRange(date, date)
        val workTimeByDate = projectRepository.getWorkTimeByDate(date)

        val projectNames = projectRepository.getProjectNames()

        val settings = settingsRepository.getEffectiveSettingsForDate(date)
        val fallbackSettings = if (settings?.dailyWorkTimeEstimate.isNullOrEmpty() ||
            settings.dailyWorkTimeEstimate == ZERO_TIME) {
            settingsRepository.getSettings()
        } else {
            null
        }
        val globalDailyWorkTimeEstimate = fallbackSettings?.dailyWorkTimeEstimate
            ?.takeIf { it != ZERO_TIME && it.isNotEmpty() }
            ?: DEFAULT_DAILY_WORK_TIME
        val workTimeByDateEstimate = settings?.dailyWorkTimeEstimate
            ?.takeIf { it != ZERO_TIME && it.isNotEmpty() }
            ?: globalDailyWorkTimeEstimate
        val initialFlexTimeTotal = settings?.initialFlexTimeTotal?.takeIf {
            it != ZERO_TIME && it.isNotEmpty()
        } ?: ZERO_TIME
        val persistedFlexTimeDeltaTotal = settingsRepository.getCalculatedFlextimeTotal()

        return WorkdayScreenData(
            workTimeByDate = workTimeByDate,
            workTimeByDateEstimate = workTimeByDateEstimate,
            initialFlexTimeTotal = initialFlexTimeTotal,
            flexTimeTotal = WorkTimeCalculator.calculateFlexTime(
                initialTime = initialFlexTimeTotal,
                addedTime = persistedFlexTimeDeltaTotal
            ),
            projects = projects,
            projectNames = projectNames
        )
    }
}

data class WorkdayScreenData(
    val workTimeByDate: String,
    val workTimeByDateEstimate: String,
    val initialFlexTimeTotal: String,
    val flexTimeTotal: String,
    val projects: List<SingleProjectState>,
    val projectNames: List<String>
)

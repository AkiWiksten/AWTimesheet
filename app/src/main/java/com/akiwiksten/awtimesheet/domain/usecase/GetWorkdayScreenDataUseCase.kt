package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.calculator.WorkTimeCalculator
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import javax.inject.Inject

class GetWorkdayScreenDataUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
    private val workdayRepository: WorkdayRepository
) {
    suspend operator fun invoke(date: String): WorkdayScreenData {
        val projects = projectRepository.getProjectsByDateRange(date, date)
        val workTimeByDate = projectRepository.getWorkTimeByDate(date)

        val projectNames = projectRepository.getProjectNames()

        val globalSettings = settingsRepository.getSettings()
        val settings = settingsRepository.getEffectiveSettingsForDate(date)
        val globalDailyWorkTimeEstimate = globalSettings?.dailyWorkTimeEstimate
            ?.ifEmpty { DEFAULT_DAILY_WORK_TIME }
            ?: DEFAULT_DAILY_WORK_TIME
        val workTimeByDateEstimate =
            settings?.dailyWorkTimeEstimate?.ifEmpty { globalDailyWorkTimeEstimate }
                ?: globalDailyWorkTimeEstimate
        val initialFlexTimeTotal = settings?.initialFlexTimeTotal?.ifEmpty { ZERO_TIME } ?: ZERO_TIME
        val calculatedFlexTimeTotal = settingsRepository.getCalculatedFlextimeTotal()

        return WorkdayScreenData(
            workTimeByDate = workTimeByDate,
            workTimeByDateEstimate = workTimeByDateEstimate,
            initialFlexTimeTotal = initialFlexTimeTotal,
            flexTimeTotal = WorkTimeCalculator.calculateFlexTime(
                initialTime = initialFlexTimeTotal,
                addedTime = calculatedFlexTimeTotal
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

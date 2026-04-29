package com.akiwiksten.worktime30.domain.usecase

import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
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
        var calculatedFlexTimeFromAllWorkdays = ZERO_TIME
        val workdayRows = workdayRepository.getWorkdaysByDateRange(ALL_DATES_START, ALL_DATES_END)
        for (row in workdayRows) {
            val workedTime = projectRepository.getWorkTimeByDate(row.date)
            val flexTimeToday = WorkTimeCalculator.calculateFlexTime(
                initialTime = workedTime,
                addedTime = "-${row.workTimeByDateEstimate}"
            )
            calculatedFlexTimeFromAllWorkdays = WorkTimeCalculator.calculateFlexTime(
                calculatedFlexTimeFromAllWorkdays,
                flexTimeToday
            )
        }

        return WorkdayScreenData(
            workTimeByDate = workTimeByDate,
            workTimeByDateEstimate = workTimeByDateEstimate,
            initialFlexTimeTotal = initialFlexTimeTotal,
            calculatedFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
                initialTime = initialFlexTimeTotal,
                addedTime = calculatedFlexTimeFromAllWorkdays
            ),
            projects = projects,
            projectNames = projectNames
        )
    }

    private companion object {
        const val ALL_DATES_START = "0000-01-01"
        const val ALL_DATES_END = "9999-12-31"
    }
}

data class WorkdayScreenData(
    val workTimeByDate: String,
    val workTimeByDateEstimate: String,
    val initialFlexTimeTotal: String,
    val calculatedFlexTimeTotal: String,
    val projects: List<SingleProjectState>,
    val projectNames: List<String>
)

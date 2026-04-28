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
        val projectTime = projectRepository.getProjectTimeSumByDate(date)

        val projectNames = projectRepository.getProjectNames()

        val workTypes = settingsRepository.getWorkTypes()
        val workStats = settingsRepository.getWorkStatsByDate(date)
        val workTimeTodayEstimate =
            workStats?.dailyWorkTimeEstimate?.ifEmpty { DEFAULT_DAILY_WORK_TIME } ?: DEFAULT_DAILY_WORK_TIME
        val initialFlexTimeTotal = workStats?.initialFlexTimeTotal?.ifEmpty { ZERO_TIME } ?: ZERO_TIME
        var calculatedFlexTimeFromAllWorkdays = ZERO_TIME
        val workdayRows = workdayRepository.getWorkdaysByDateRange(ALL_DATES_START, ALL_DATES_END)
        for (row in workdayRows) {
            val workedTime = projectRepository.getProjectTimeSumByDate(row.date)
            val dailyFlex = WorkTimeCalculator.calculateFlexTime(
                initialTime = workedTime,
                addedTime = "-${row.workTimeTodayEstimate}"
            )
            calculatedFlexTimeFromAllWorkdays = WorkTimeCalculator.calculateFlexTime(
                calculatedFlexTimeFromAllWorkdays,
                dailyFlex
            )
        }

        return WorkdayScreenData(
            projectTime = projectTime,
            workTimeTodayEstimate = workTimeTodayEstimate,
            initialFlexTimeTotal = initialFlexTimeTotal,
            calculatedFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
                initialTime = initialFlexTimeTotal,
                addedTime = calculatedFlexTimeFromAllWorkdays
            ),
            projects = projects,
            projectNames = projectNames,
            workTypes = workTypes
        )
    }

    private companion object {
        const val ALL_DATES_START = "0000-01-01"
        const val ALL_DATES_END = "9999-12-31"
    }
}

data class WorkdayScreenData(
    val projectTime: String,
    val workTimeTodayEstimate: String,
    val initialFlexTimeTotal: String,
    val calculatedFlexTimeTotal: String,
    val projects: List<SingleProjectState>,
    val projectNames: List<String>,
    val workTypes: List<String>
)

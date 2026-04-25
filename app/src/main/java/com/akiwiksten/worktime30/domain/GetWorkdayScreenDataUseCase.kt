package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.feature.workday.SingleProjectState
import javax.inject.Inject

class GetWorkdayScreenDataUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
    private val projectDetailsRepository: ProjectDetailsRepository
) {
    suspend operator fun invoke(date: String): WorkdayScreenData {
        val projects = projectRepository.getProjectsByDateRange(date, date)
        val projectTime = projects.fold(ZERO_TIME) { acc, project ->
            WorkTimeCalculator.calculateFlexTime(acc, project.projectTime)
        }

        val projectNames = projectRepository.getProjectNames()

        val workTypes = settingsRepository.getWorkTypes()
        val workStats = projectDetailsRepository.getWorkStats()
        val workTimeTodayEstimate =
            workStats?.dailyWorkTime?.ifEmpty { DEFAULT_DAILY_WORK_TIME } ?: DEFAULT_DAILY_WORK_TIME
        val initialFlexTimeTotal = workStats?.initialFlexTimeTotal?.ifEmpty { ZERO_TIME } ?: ZERO_TIME
        val calculatedFlexTimeFromAllWorkdays = projectRepository
            .getProjectsByDateRange(ALL_DATES_START, ALL_DATES_END)
            .filter { it.projectTime.isNotEmpty() && it.projectTime != ZERO_TIME }
            .groupBy { it.date }
            .values
            .fold(ZERO_TIME) { acc, projectsForDate ->
                val totalProjectTimeForDate = projectsForDate.fold(ZERO_TIME) { dayAcc, project ->
                    WorkTimeCalculator.calculateFlexTime(dayAcc, project.projectTime)
                }
                val dailyFlex = WorkTimeCalculator.calculateFlexTime(
                    initialTime = totalProjectTimeForDate,
                    addedTime = "-$workTimeTodayEstimate"
                )
                WorkTimeCalculator.calculateFlexTime(acc, dailyFlex)
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

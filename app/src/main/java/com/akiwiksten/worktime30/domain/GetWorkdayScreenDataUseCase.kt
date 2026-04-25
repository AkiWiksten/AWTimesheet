package com.akiwiksten.worktime30.domain

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
        val initialFlexTimeTotal = workStats?.initialFlexTimeTotal?.ifEmpty { ZERO_TIME } ?: ZERO_TIME
        val calculatedFlexTimeFromAllWorkdays = projectDetailsRepository
            .getProjectDetailsByDateRange(ALL_DATES_START, ALL_DATES_END)
            .groupBy { it.date }
            .values
            .fold(ZERO_TIME) { acc, detailsForDate ->
                val dailyFlex = detailsForDate.firstOrNull()?.flexTimeToday?.ifEmpty { ZERO_TIME } ?: ZERO_TIME
                WorkTimeCalculator.calculateFlexTime(acc, dailyFlex)
            }

        return WorkdayScreenData(
            projectTime = projectTime,
            dailyWorkTime = workStats?.dailyWorkTime?.ifEmpty { DEFAULT_DAILY_WORK_TIME } ?: DEFAULT_DAILY_WORK_TIME,
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
        const val DEFAULT_DAILY_WORK_TIME = "07:30"
        const val ALL_DATES_START = "0000-01-01"
        const val ALL_DATES_END = "9999-12-31"
    }
}

data class WorkdayScreenData(
    val projectTime: String,
    val dailyWorkTime: String,
    val initialFlexTimeTotal: String,
    val calculatedFlexTimeTotal: String,
    val projects: List<SingleProjectState>,
    val projectNames: List<String>,
    val workTypes: List<String>
)

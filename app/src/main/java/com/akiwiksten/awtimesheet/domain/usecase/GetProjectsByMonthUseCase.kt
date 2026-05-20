package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.calculator.WorkTimeCalculator
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import java.time.LocalDate
import javax.inject.Inject

data class ProjectsByMonthResult(
    val projects: List<SingleProjectState>,
    val endOfMonth: String,
    val initialFlexTimeTotal: String = ZERO_TIME,
    val totalFlexTimeTotal: String = ZERO_TIME
)

class GetProjectsByMonthUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
    private val workdayRepository: WorkdayRepository
) {
    suspend operator fun invoke(date: String): ProjectsByMonthResult {
        val parsedDate = LocalDate.parse(date)
        val startOfMonth = parsedDate.withDayOfMonth(1).toString()
        val endOfMonth = parsedDate
            .withDayOfMonth(parsedDate.month.length(parsedDate.isLeapYear))
            .toString()
        val projects = projectRepository.getProjectsByDateRange(startOfMonth, endOfMonth)

        // Calculate flex time totals
        val globalSettings = settingsRepository.getSettings()
        val initialFlexTimeTotal = globalSettings?.initialFlexTimeTotal ?: ZERO_TIME

        var calculatedFlexTimeFromAllWorkdays = ZERO_TIME
        val workdayRows = workdayRepository.getWorkdaysByDateRange("0000-01-01", "9999-12-31")
        for (row in workdayRows) {
            val effectiveWorkTimeEstimate = row.workTimeByDateEstimate
                .ifBlank { globalSettings?.dailyWorkTimeEstimate ?: DEFAULT_DAILY_WORK_TIME }
            val workedTime = projectRepository.getWorkTimeByDate(row.date)
            val flexTimeByDate = WorkTimeCalculator.calculateFlexTime(
                initialTime = workedTime,
                addedTime = "-$effectiveWorkTimeEstimate"
            )
            calculatedFlexTimeFromAllWorkdays = WorkTimeCalculator.calculateFlexTime(
                calculatedFlexTimeFromAllWorkdays,
                flexTimeByDate
            )
        }

        val calculatedFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
            initialTime = initialFlexTimeTotal,
            addedTime = calculatedFlexTimeFromAllWorkdays
        )

        return ProjectsByMonthResult(
            projects = projects,
            endOfMonth = endOfMonth,
            initialFlexTimeTotal = initialFlexTimeTotal,
            totalFlexTimeTotal = calculatedFlexTimeTotal
        )
    }

    private companion object {
        const val DEFAULT_DAILY_WORK_TIME = "07:30"
    }
}

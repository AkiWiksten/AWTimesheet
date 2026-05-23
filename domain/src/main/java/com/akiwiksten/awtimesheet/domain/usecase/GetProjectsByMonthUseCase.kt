package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import java.time.LocalDate
import javax.inject.Inject

data class ProjectsByMonthResult(
    val projects: List<SingleProjectState>,
    val endOfMonth: String,
    val flexTimeTotal: String = ZERO_TIME
)

class GetProjectsByMonthUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val settingsRepository: SettingsRepository,
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
        val calculatedFlexTimeTotal = settingsRepository.getCalculatedFlextimeTotal()
        val flexTimeTotal = WorkTimeCalculator.calculateFlexTime(
            initialTime = initialFlexTimeTotal,
            addedTime = calculatedFlexTimeTotal
        )
        return ProjectsByMonthResult(
            projects = projects,
            endOfMonth = endOfMonth,
            flexTimeTotal = flexTimeTotal
        )
    }
}

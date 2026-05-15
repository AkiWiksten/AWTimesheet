package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.calculator.WorkTimeCalculator
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * UseCase that provides all calculated data required by the Calendar screen.
 */
class GetCalendarDataUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(date: String): CalendarData {
        val initial = LocalDate.parse(date)
        val startMonth = initial.withDayOfMonth(1).toString()
        val lastDay = initial.month.length(initial.isLeapYear)
        val endMonth = initial.withDayOfMonth(lastDay).toString()

        val startOfWeek = initial.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = startOfWeek.plusDays(6)

        // Optimization: Fetch all needed data for the month in one go
        val projectTimesMonth = projectRepository.getProjectsByDateRange(startMonth, endMonth)

        val timePerMonth = calculateTotalTime(projectTimesMonth)

        // Filter for week from already fetched monthly data if possible, else fetch
        val projectTimesWeek = if (
            startOfWeek.toString() >= startMonth &&
            endOfWeek.toString() <= endMonth
        ) {
            val startStr = startOfWeek.toString()
            val endStr = endOfWeek.toString()
            projectTimesMonth.filter { it.date in startStr..endStr }
        } else {
            projectRepository.getProjectsByDateRange(startOfWeek.toString(), endOfWeek.toString())
        }

        val timePerWeek = calculateTotalTime(projectTimesWeek)

        val timePerDay = calculateTotalTime(
            projectTimesMonth.filter { it.date == date }
        )

        val datesWithWork = projectTimesMonth.map { it.date }.toSet()

        return CalendarData(
            timePerMonth = timePerMonth,
            timePerWeek = timePerWeek,
            timePerDay = timePerDay,
            datesWithWork = datesWithWork
        )
    }

    private fun calculateTotalTime(projects: List<SingleProjectState>): String {
        var totalTime = ZERO_TIME

        // Sum up projects
        for (project in projects) {
            totalTime = WorkTimeCalculator.calculateFlexTime(totalTime, project.projectTime)
        }

        return totalTime
    }
}

data class CalendarData(
    val timePerMonth: String,
    val timePerWeek: String,
    val timePerDay: String,
    val datesWithWork: Set<String> = emptySet()
)

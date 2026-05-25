package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
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

        // Fetch month data once and derive day/week/month summaries from it.
        val projectTimesMonth = projectRepository.getProjectsByDateRange(startMonth, endMonth)
        val startOfWeekStr = startOfWeek.toString()
        val endOfWeekStr = endOfWeek.toString()

        // If the week crosses month boundaries, fetch exact week range once.
        val projectTimesWeek = if (
            startOfWeek.toString() >= startMonth &&
            endOfWeek.toString() <= endMonth
        ) {
            null
        } else {
            projectRepository.getProjectsByDateRange(startOfWeekStr, endOfWeekStr)
        }

        var timePerMonth = ZERO_TIME
        var timePerWeekFromMonth = ZERO_TIME
        var timePerDay = ZERO_TIME
        val datesWithWork = mutableSetOf<String>()

        for (project in projectTimesMonth) {
            val projectDate = project.date
            val projectTime = project.projectTime
            datesWithWork += projectDate
            timePerMonth = WorkTimeCalculator.calculateFlexTime(timePerMonth, projectTime)

            if (projectDate in startOfWeekStr..endOfWeekStr) {
                timePerWeekFromMonth = WorkTimeCalculator.calculateFlexTime(timePerWeekFromMonth, projectTime)
            }
            if (projectDate == date) {
                timePerDay = WorkTimeCalculator.calculateFlexTime(timePerDay, projectTime)
            }
        }

        val timePerWeek = projectTimesWeek?.let(::calculateTotalTime) ?: timePerWeekFromMonth

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

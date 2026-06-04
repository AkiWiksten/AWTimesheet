package com.akiwiksten.awtimesheet.domain.usecase

import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * UseCase that provides all calculated data required by the Calendar screen.
 */
class GetCalendarDataUseCase @Inject constructor(
    private val projectRepository: ProjectRepository
) {
    private var cachedMonth: YearMonth? = null
    private var cachedTimePerMonth: String = ZERO_TIME
    private var cachedDatesWithWork: Set<String> = emptySet()

    suspend operator fun invoke(
        date: String,
        workTimeByDateChange: String = ZERO_TIME,
        forceMonthRecalculation: Boolean = false
    ): CalendarData {
        val initial = LocalDate.parse(date)
        val requestedMonth = YearMonth.from(initial)
        val startMonth = initial.withDayOfMonth(1).toString()
        val lastDay = initial.month.length(initial.isLeapYear)
        val endMonth = initial.withDayOfMonth(lastDay).toString()

        val startOfWeek = initial.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = startOfWeek.plusDays(6)
        val startOfWeekStr = startOfWeek.toString()
        val endOfWeekStr = endOfWeek.toString()

        val useCachedMonthTotal = cachedMonth == requestedMonth && !forceMonthRecalculation

        val timePerMonth: String
        val datesWithWork: Set<String>

        if (!useCachedMonthTotal) {
            val projectTimesMonth = projectRepository.getProjectsByDateRange(startMonth, endMonth)
            timePerMonth = projectTimesMonth.fold(ZERO_TIME) { acc, project ->
                WorkTimeCalculator.calculateFlexTime(acc, project.projectTime)
            }
            datesWithWork = projectTimesMonth.mapTo(mutableSetOf()) { it.date }
            cachedMonth = requestedMonth
            cachedTimePerMonth = timePerMonth
            cachedDatesWithWork = datesWithWork
        } else {
            timePerMonth = if (workTimeByDateChange != ZERO_TIME) {
                WorkTimeCalculator.calculateFlexTime(cachedTimePerMonth, workTimeByDateChange)
                    .also { cachedTimePerMonth = it }
            } else {
                cachedTimePerMonth
            }
            datesWithWork = cachedDatesWithWork
        }

        val timePerWeek = calculateTotalTime(
            projectRepository.getProjectsByDateRange(startOfWeekStr, endOfWeekStr)
        )
        val timePerDay = calculateTotalTime(
            projectRepository.getProjectsByDateRange(date, date)
        )

        return CalendarData(
            timePerMonth = timePerMonth,
            timePerWeek = timePerWeek,
            timePerDay = timePerDay,
            datesWithWork = datesWithWork
        )
    }

    private fun calculateTotalTime(projects: List<SingleProjectState>): String =
        projects.fold(ZERO_TIME) { acc, project ->
            WorkTimeCalculator.calculateFlexTime(acc, project.projectTime)
        }
}

data class CalendarData(
    val timePerMonth: String,
    val timePerWeek: String,
    val timePerDay: String,
    val datesWithWork: Set<String> = emptySet()
)

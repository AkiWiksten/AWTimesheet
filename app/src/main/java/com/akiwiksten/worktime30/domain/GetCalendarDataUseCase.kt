package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.WorkdayRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * UseCase that provides all calculated data and work logs required by the Calendar screen.
 */
class GetCalendarDataUseCase @Inject constructor(
    private val workdayRepository: WorkdayRepository,
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
        val workdaysMonth = workdayRepository.getWorkdaysByDateRange(startMonth, endMonth)
        val projectTimesMonth = projectRepository.getProjectsByDateRange(startMonth, endMonth)

        val timePerMonth = calculateTotalTime(workdaysMonth, projectTimesMonth)

        // Filter for week from already fetched monthly data if possible, else fetch
        val (workdaysWeek, projectTimesWeek) = if (
            startOfWeek.toString() >= startMonth &&
            endOfWeek.toString() <= endMonth
        ) {
            val startStr = startOfWeek.toString()
            val endStr = endOfWeek.toString()
            workdaysMonth.filter { it.date in startStr..endStr } to
                projectTimesMonth.filter { it.date in startStr..endStr }
        } else {
            workdayRepository.getWorkdaysByDateRange(startOfWeek.toString(), endOfWeek.toString()) to
                projectRepository.getProjectsByDateRange(startOfWeek.toString(), endOfWeek.toString())
        }

        val timePerWeek = calculateTotalTime(workdaysWeek, projectTimesWeek)

        val timePerDay = calculateTotalTime(
            workdaysMonth.filter { it.date == date },
            projectTimesMonth.filter { it.date == date }
        )

        return CalendarData(
            timePerMonth = timePerMonth,
            timePerWeek = timePerWeek,
            timePerDay = timePerDay,
            workdaysMonth = workdaysMonth
        )
    }

    private fun calculateTotalTime(workdays: List<WorkdayEntity>, projects: List<ProjectEntity>): String {
        var totalTime = ZERO_TIME

        // Map work logs by date for fast lookup
        val workdayDates = workdays.associateBy { it.date }

        // Sum up work days
        for (workday in workdays) {
            totalTime = WorkTimeCalculator.calculateWorkTimeBalance(totalTime, workday.workTimeToday)
        }

        // Sum up projects for dates that DON'T have a workday entry
        projects.filter { it.date !in workdayDates }.forEach { project ->
            totalTime = WorkTimeCalculator.calculateWorkTimeBalance(totalTime, project.projectTime)
        }

        return totalTime
    }
}

data class CalendarData(
    val timePerMonth: String,
    val timePerWeek: String,
    val timePerDay: String,
    val workdaysMonth: List<WorkdayEntity>
)

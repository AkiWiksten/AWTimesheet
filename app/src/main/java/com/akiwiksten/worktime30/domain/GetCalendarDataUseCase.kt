package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.WorkDayRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/**
 * UseCase that provides all calculated data and work logs required by the Calendar screen.
 */
class GetCalendarDataUseCase @Inject constructor(
    private val workDayRepository: WorkDayRepository,
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
        val workDaysMonth = workDayRepository.getWorkDaysByDateRange(startMonth, endMonth)
        val projectTimesMonth = projectRepository.getProjectsByDateRange(startMonth, endMonth)

        val timePerMonth = calculateTotalTime(workDaysMonth, projectTimesMonth)

        // Filter for week from already fetched monthly data if possible, else fetch
        val (workDaysWeek, projectTimesWeek) = if (
            startOfWeek.toString() >= startMonth && 
            endOfWeek.toString() <= endMonth
        ) {
            val startStr = startOfWeek.toString()
            val endStr = endOfWeek.toString()
            workDaysMonth.filter { it.date in startStr..endStr } to 
                projectTimesMonth.filter { it.date in startStr..endStr }
        } else {
            workDayRepository.getWorkDaysByDateRange(startOfWeek.toString(), endOfWeek.toString()) to
                projectRepository.getProjectsByDateRange(startOfWeek.toString(), endOfWeek.toString())
        }
        
        val timePerWeek = calculateTotalTime(workDaysWeek, projectTimesWeek)

        val timePerDay = workDaysMonth.find { it.date == date }?.workTimeToday 
            ?: calculateTotalTime(emptyList(), projectTimesMonth.filter { it.date == date })

        return CalendarData(
            timePerMonth = timePerMonth,
            timePerWeek = timePerWeek,
            timePerDay = timePerDay,
            workDaysMonth = workDaysMonth
        )
    }

    private fun calculateTotalTime(workDays: List<WorkDayEntity>, projects: List<ProjectEntity>): String {
        var totalTime = ZERO_TIME
        
        // Map work logs by date for fast lookup
        val workDayDates = workDays.associateBy { it.date }
        
        // Sum up work days
        for (workDay in workDays) {
            totalTime = WorkTimeCalculator.calculateTotalMinutes(
                initialTime = totalTime,
                addedTime = workDay.workTimeToday,
                isInitialTimeNegative = false,
                isAddedTimeNegative = false
            )
        }

        // Sum up projects for dates that DON'T have a work day entry
        projects.filter { it.date !in workDayDates }.forEach { project ->
            val projectDuration = WorkTimeCalculator.calculateWorkTimeBalance(
                project.projectEndTime, "-" + project.projectStartTime
            )
            totalTime = WorkTimeCalculator.calculateWorkTimeBalance(totalTime, projectDuration)
        }
        
        return totalTime
    }
}

data class CalendarData(
    val timePerMonth: String,
    val timePerWeek: String,
    val timePerDay: String,
    val workDaysMonth: List<WorkDayEntity>
)

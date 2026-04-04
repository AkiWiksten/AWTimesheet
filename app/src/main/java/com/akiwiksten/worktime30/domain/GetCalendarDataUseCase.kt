package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.WorkTimeCalculator.parseDate
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.WorkDayRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import javax.inject.Inject

/**
 * UseCase that provides all calculated data and work logs required by the Calendar screen.
 */
class GetCalendarDataUseCase @Inject constructor(
    private val workDayRepository: WorkDayRepository,
    private val projectRepository: ProjectRepository
) {
    suspend operator fun invoke(date: String): CalendarData {
        val monthlyResult = calculateMonthlyWorkTime(date)
        return CalendarData(
            timePerMonth = monthlyResult.totalTime,
            timePerWeek = calculateWeeklyWorkTime(date),
            timePerDay = calculateTimePerDay(date),
            workDaysMonth = monthlyResult.workDays
        )
    }

    private suspend fun calculateMonthlyWorkTime(selectedDate: String): MonthlyWorkTimeResult {
        val initial = LocalDate.parse(selectedDate)
        val startMonth = initial.withDayOfMonth(1).toString()
        val endMonth = initial.withDayOfMonth(initial.month.length(initial.isLeapYear)).toString()
        
        val workDays = workDayRepository.getWorkDaysByDateRange(startMonth, endMonth)
        val projectTimesMonth = projectRepository.getProjectsByDateRange(startMonth, endMonth)
        
        var workTimeMonth = ZERO_TIME
        val endDayOfMonth = parseDate(endMonth).toInt()
        for (day in 1..endDayOfMonth) {
            val dayStr = String.format(Locale.US, "%02d", day)
            val workDay = workDays.find { parseDate(it.date) == dayStr }
            if (workDay != null) {
                workTimeMonth = WorkTimeCalculator.calculateTotalMinutes(
                    initialTime = workTimeMonth,
                    addedTime = workDay.workTimeToday,
                    isInitialTimeNegative = false,
                    isAddedTimeNegative = false
                )
            } else {
                projectTimesMonth.filter { parseDate(it.date) == dayStr }
                    .forEach { project ->
                        val projectDuration = WorkTimeCalculator.calculateWorkTimeBalance(
                            project.projectEndTime, "-" + project.projectStartTime
                        )
                        workTimeMonth = WorkTimeCalculator.calculateWorkTimeBalance(
                            workTimeMonth, projectDuration
                        )
                    }
            }
        }
        return MonthlyWorkTimeResult(workTimeMonth, workDays)
    }

    private suspend fun calculateWeeklyWorkTime(date: String): String {
        val initial = LocalDate.parse(date)
        val startOfWeek = initial.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = startOfWeek.plusDays(6)
        
        val workDaysWeek = workDayRepository.getWorkDaysByDateRange(startOfWeek.toString(), endOfWeek.toString())
        val projectTimesWeek = projectRepository.getProjectsByDateRange(startOfWeek.toString(), endOfWeek.toString())
        
        var workTimeWeek = ZERO_TIME
        val weekDates = (0..6L).map { startOfWeek.plusDays(it).toString() }

        for (dateStr in weekDates) {
            val workDay = workDaysWeek.find { it.date == dateStr }
            if (workDay != null) {
                workTimeWeek = WorkTimeCalculator.calculateTotalMinutes(
                    initialTime = workTimeWeek,
                    addedTime = workDay.workTimeToday,
                    isInitialTimeNegative = false,
                    isAddedTimeNegative = false
                )
            } else {
                projectTimesWeek.filter { it.date == dateStr }
                    .forEach { project ->
                        val projectDuration = WorkTimeCalculator.calculateWorkTimeBalance(
                            project.projectEndTime, "-" + project.projectStartTime
                        )
                        workTimeWeek = WorkTimeCalculator.calculateWorkTimeBalance(
                            workTimeWeek, projectDuration
                        )
                    }
            }
        }
        return workTimeWeek
    }

    private suspend fun calculateTimePerDay(date: String): String {
        val workDay = workDayRepository.getWorkDay(date)
        return if (workDay != null) {
            workDay.workTimeToday
        } else {
            val projectsPerDay = projectRepository.getProjectsByDateRange(date, date)
            var projectTimeDay = ZERO_TIME
            for (project in projectsPerDay) {
                val projectDuration = WorkTimeCalculator.calculateWorkTimeBalance(
                    project.projectEndTime, "-" + project.projectStartTime
                )
                projectTimeDay = WorkTimeCalculator.calculateWorkTimeBalance(
                    projectTimeDay, projectDuration
                )
            }
            projectTimeDay
        }
    }

    private data class MonthlyWorkTimeResult(val totalTime: String, val workDays: List<WorkDayEntity>)
}

data class CalendarData(
    val timePerMonth: String,
    val timePerWeek: String,
    val timePerDay: String,
    val workDaysMonth: List<WorkDayEntity>
)

package com.akiwiksten.worktime30.feature.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.WorkTimeCalculator.parseDate
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.AppDatabase
import com.akiwiksten.worktime30.data.database.WorkDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@Suppress("TooManyFunctions")
@HiltViewModel
class CalendarViewModel @Inject constructor() : ViewModel() {
    private val _ctx = MutableStateFlow<Context?>(null)
    private val _date = MutableStateFlow("")
    val date = _date.asStateFlow()
    private val _timePerMonth = MutableStateFlow("")
    val timePerMonth = _timePerMonth.asStateFlow()
    private val _timePerWeek = MutableStateFlow("")
    val timePerWeek = _timePerWeek.asStateFlow()
    private val _timePerDay = MutableStateFlow("")
    val timePerDay = _timePerDay.asStateFlow()
    private val _workDaysMonth = MutableStateFlow<List<WorkDay>>(emptyList())

    init {
        _date.value = currentDate()
    }

    fun setCtx(ctx: Context) {
        _ctx.value = ctx
    }

    fun convertMillisToDate(millis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date(millis))
    }

    private fun currentDate(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val current = LocalDateTime.now().format(formatter)
        return current
    }

    fun setDate(date0: String) {
        _date.value = date0
    }

    fun calculateSums(date: String) {
        viewModelScope.launch {
            calculateMonthlyWorkTime(date)
            calculateWeeklyWorkTime(date)
            setTimePerDay(date)
        }
    }

    private suspend fun calculateMonthlyWorkTime(selectedDate: String) {
        val initial = LocalDate.parse(selectedDate)
        val startMonth = initial.withDayOfMonth(1).toString()
        val endMonth = initial.withDayOfMonth(initial.month.length(initial.isLeapYear)).toString()
        _workDaysMonth.value = AppDatabase.getInstance(_ctx.value!!).workDayDao()
            .getWorkDaysByDateRange(startMonth, endMonth)
        val projectTimesMonth = AppDatabase.getInstance(_ctx.value!!).projectDao()
            .getProjectsByDateRange(startMonth, endMonth)
        var workTimeMonth = ZERO_TIME
        for (day in 1..parseDate(endMonth).toInt()) {
            val workDay = _workDaysMonth.value.find { w -> parseDate(w.date).toInt() == day }
            if (workDay != null) {
                workTimeMonth = WorkTimeCalculator.calculateTotalMinutes(
                    initialTime = workTimeMonth,
                    addedTime = workDay.workTimeToday,
                    isInitialTimeNegative = false,
                    isAddedTimeNegative = false
                )
            } else {
                val projectsDay = projectTimesMonth.filter {
                        p ->
                    parseDate(p.date).toInt() == day
                }
                for (project in projectsDay) {
                    workTimeMonth = WorkTimeCalculator.calculateWorkTimeBalance(
                        workTimeMonth,
                        project.projectEndTime
                    )
                    workTimeMonth = WorkTimeCalculator.calculateWorkTimeBalance(
                        workTimeMonth,
                        "-" + project.projectStartTime
                    )
                }
            }
        }
        _timePerMonth.value = workTimeMonth
    }

    @Suppress("MagicNumber", "LongMethod")
    private suspend fun calculateWeeklyWorkTime(date: String) {
        val initial = LocalDate.parse(date)
        var workTimeWeek = ZERO_TIME
        val firstDateOfWeek = initial.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val workDaysWeek = AppDatabase.getInstance(_ctx.value!!).workDayDao()
            .getWorkDaysByDateRange(
                firstDateOfWeek.toString(),
                firstDateOfWeek.plusDays(6).toString()
            )
        val firstDayOfWeek = parseDate(firstDateOfWeek.toString()).toInt()
        val lastDayOfWeek = parseDate(firstDateOfWeek.plusDays(6).toString()).toInt()
        val projectTimesWeek = AppDatabase.getInstance(_ctx.value!!).projectDao()
            .getProjectsByDateRange(
                firstDateOfWeek.toString(),
                firstDateOfWeek.plusDays(6).toString()
            )
        val allWeekDays: MutableList<Int> = mutableListOf()

        if (firstDayOfWeek > lastDayOfWeek) {
            val initial0 = initial.minusMonths(1)
            val endMonth = initial0.withDayOfMonth(initial0.month.length(initial0.isLeapYear))
            val endMonthDay = parseDate(endMonth.toString()).toInt()
            for (day in firstDayOfWeek..endMonthDay) {
                allWeekDays.add(day)
            }
            for (day in 1..lastDayOfWeek) {
                allWeekDays.add(day)
            }
        } else {
            for (day in firstDayOfWeek..lastDayOfWeek) {
                allWeekDays.add(day)
            }
        }

        for (day in allWeekDays) {
            val workDay = workDaysWeek.find { w -> parseDate(w.date).toInt() == day }
            if (workDay != null) {
                workTimeWeek = WorkTimeCalculator.calculateTotalMinutes(
                    initialTime = workTimeWeek,
                    addedTime = workDay.workTimeToday,
                    isInitialTimeNegative = false,
                    isAddedTimeNegative = false
                )
            } else {
                val projectsDay = projectTimesWeek.filter {
                        p ->
                    parseDate(p.date).toInt() == day
                }
                for (project in projectsDay) {
                    workTimeWeek = WorkTimeCalculator.calculateWorkTimeBalance(
                        workTimeWeek,
                        project.projectEndTime
                    )
                    workTimeWeek = WorkTimeCalculator.calculateWorkTimeBalance(
                        workTimeWeek,
                        "-" + project.projectStartTime
                    )
                }
            }
        }
        _timePerWeek.value = workTimeWeek
    }

    private suspend fun setTimePerDay(date: String) {
        var projectTimeDay = ZERO_TIME
        val workDay = AppDatabase
            .getInstance(_ctx.value!!)
            .workDayDao()
            .loadWorkDay(date)
        val projectsPerDay = AppDatabase
            .getInstance(_ctx.value!!)
            .projectDao()
            .loadProjectsByDate(date)
        if (workDay != null) {
            _timePerDay.value = workDay.workTimeToday
        } else {
            for (project in projectsPerDay) {
                projectTimeDay = WorkTimeCalculator.calculateWorkTimeBalance(
                    projectTimeDay,
                    project.projectEndTime
                )
                projectTimeDay = WorkTimeCalculator.calculateWorkTimeBalance(
                    projectTimeDay,
                    "-" + project.projectStartTime
                )
            }
            _timePerDay.value = projectTimeDay
        }
    }
}

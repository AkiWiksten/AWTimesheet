package com.akiwiksten.worktime30.core

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

/**
 * Pure logic for time calculations and work balance.
 * This class is stateless and provides calculation results back to the caller.
 */
@Suppress("TooManyFunctions")
object WorkTimeCalculator {

    private val timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
    private val dayFormatter = DateTimeFormatter.ofPattern("dd")

    fun parseDate(workDay: String): String {
        // Optimization: LocalDate.parse is relatively slow. 
        // If workDay is always in ISO format (yyyy-MM-dd), we can just take the last 2 chars.
        return if (workDay.length >= 10 && workDay[4] == '-' && workDay[7] == '-') {
            workDay.substring(8, 10)
        } else {
            val formattedDate = LocalDate.parse(workDay)
            dayFormatter.format(formattedDate)
        }
    }

    fun stringToLocalTime(time: String): LocalTime {
        return LocalTime.parse(time, timeFormatter)
    }

    fun calculateWorkTimeBalance(initialTime: String, addedTime: String): String {
        val isInitialNegative = initialTime.startsWith("-")
        val isAddedNegative = addedTime.startsWith("-")
        
        val cleanInitial = if (isInitialNegative) initialTime.substring(1) else initialTime
        val cleanAdded = if (isAddedNegative) addedTime.substring(1) else addedTime

        return calculateTotalMinutes(
            initialTime = cleanInitial,
            addedTime = cleanAdded,
            isInitialTimeNegative = isInitialNegative,
            isAddedTimeNegative = isAddedNegative
        )
    }

    fun calculateTotalMinutes(
        initialTime: String,
        addedTime: String,
        isInitialTimeNegative: Boolean,
        isAddedTimeNegative: Boolean
    ): String {
        val initialTotalMinutes = timeToMinutes(initialTime)
        val addedTotalMinutes = timeToMinutes(addedTime)

        val initialSign = if (isInitialTimeNegative) -1 else 1
        val addedSign = if (isAddedTimeNegative) -1 else 1

        val totalMinutes = (initialTotalMinutes * initialSign) + (addedTotalMinutes * addedSign)
        
        val absTotal = abs(totalMinutes)
        val hours = absTotal / MINUTES_IN_HOUR
        val minutes = absTotal % MINUTES_IN_HOUR
        
        val sign = if (totalMinutes < 0) "-" else ""
        
        // Optimization: Use StringBuilder or manual padding instead of String.format for performance
        return buildString(6) {
            append(sign)
            if (hours < 10) append('0')
            append(hours)
            append(':')
            if (minutes < 10) append('0')
            append(minutes)
        }
    }

    fun calculateStartTimeUpdate(params: StartTimeUpdateParams): TimeUpdateResult {
        return if (params.isNewDay) {
            val end = params.start.add(params.dailyWorkTime).add(params.lunchTime)
            val lunchStart = calculateHalfTime(params.start, params.dailyWorkTime)
            TimeUpdateResult(
                endTime = end.toString(),
                lunchStart = lunchStart.toString(),
                lunchEnd = lunchStart.add(params.lunchTime).toString(),
                breakStart = params.start.toString(),
                breakEnd = params.start.toString()
            )
        } else if (params.workTimeToday != LocalTime.MIDNIGHT) {
            val newWorkTimeToday = params.workTimeToday.subtract(params.start).add(params.oldStartTime)
            TimeUpdateResult(workTimeToday = newWorkTimeToday.toString())
        } else {
            TimeUpdateResult()
        }
    }

    fun calculateEndTimeUpdate(params: EndTimeUpdateParams): TimeUpdateResult {
        val newWorkTimeToday = if (params.workTimeToday == LocalTime.MIDNIGHT) {
            params.end.subtract(params.start).subtract(params.lunchEnd).add(params.lunchStart)
                .subtract(params.breakEnd).add(params.breakStart)
        } else {
            params.workTimeToday.subtract(params.oldEndTime).add(params.end)
        }
        return TimeUpdateResult(workTimeToday = newWorkTimeToday.toString())
    }

    fun calculateDailyWorkTimeUpdate(
        end: LocalTime,
        dailyWorkTime: LocalTime,
        workTimeToday: LocalTime,
        oldDailyWorkTime: LocalTime,
        isNewDay: Boolean
    ): TimeUpdateResult {
        return if (!isNewDay && workTimeToday == LocalTime.MIDNIGHT) {
            val newEnd = end.subtract(oldDailyWorkTime).add(dailyWorkTime)
            TimeUpdateResult(endTime = newEnd.toString())
        } else {
            TimeUpdateResult()
        }
    }

    fun calculateLunchStartUpdate(
        lunchStart: LocalTime,
        lunchTime: LocalTime,
        workTimeToday: LocalTime,
        oldLunchStart: LocalTime,
        currentLunchEnd: LocalTime
    ): TimeUpdateResult {
        val newLunchEnd = if (workTimeToday == LocalTime.MIDNIGHT) {
            lunchStart.add(lunchTime)
        } else {
            currentLunchEnd.subtract(oldLunchStart).add(lunchStart)
        }
        return TimeUpdateResult(lunchEnd = newLunchEnd.toString())
    }

    fun calculateLunchEndUpdate(
        end: LocalTime,
        lunchEnd: LocalTime,
        workTimeToday: LocalTime,
        oldLunchEnd: LocalTime
    ): TimeUpdateResult {
        return if (workTimeToday == LocalTime.MIDNIGHT) {
            val newEnd = end.subtract(oldLunchEnd).add(lunchEnd)
            TimeUpdateResult(endTime = newEnd.toString())
        } else {
            val newWorkTimeToday = workTimeToday.subtract(lunchEnd).add(oldLunchEnd)
            TimeUpdateResult(workTimeToday = newWorkTimeToday.toString(), calculateBalance = true)
        }
    }

    fun calculateLunchTimeUpdate(
        end: LocalTime,
        lunchStart: LocalTime,
        lunchTime: LocalTime,
        workTimeToday: LocalTime,
        oldLunchTime: LocalTime
    ): TimeUpdateResult {
        return if (workTimeToday == LocalTime.MIDNIGHT) {
            val newEnd = end.add(lunchTime).subtract(oldLunchTime)
            val newLunchEnd = lunchStart.add(lunchTime)
            TimeUpdateResult(endTime = newEnd.toString(), lunchEnd = newLunchEnd.toString())
        } else {
            TimeUpdateResult()
        }
    }

    fun calculateBreakStartUpdate(
        end: LocalTime,
        breakStart: LocalTime,
        breakEnd: LocalTime,
        workTimeToday: LocalTime,
        oldBreakStart: LocalTime
    ): TimeUpdateResult {
        return when {
            oldBreakStart == breakEnd -> TimeUpdateResult(breakEnd = breakStart.toString())
            workTimeToday == LocalTime.MIDNIGHT -> {
                val newEnd = end.subtract(oldBreakStart).add(breakEnd)
                TimeUpdateResult(endTime = newEnd.toString())
            }
            else -> {
                val newWorkTimeToday = workTimeToday.subtract(oldBreakStart).add(breakStart)
                TimeUpdateResult(workTimeToday = newWorkTimeToday.toString(), calculateBalance = true)
            }
        }
    }

    fun calculateBreakEndUpdate(
        end: LocalTime,
        workTimeToday: LocalTime,
        breakEnd: LocalTime,
        oldBreakEnd: LocalTime
    ): TimeUpdateResult {
        return if (workTimeToday == LocalTime.MIDNIGHT) {
            val newEnd = end.subtract(oldBreakEnd).add(breakEnd)
            TimeUpdateResult(endTime = newEnd.toString())
        } else {
            val newWorkTimeToday = workTimeToday.subtract(breakEnd).add(oldBreakEnd)
            TimeUpdateResult(workTimeToday = newWorkTimeToday.toString(), calculateBalance = true)
        }
    }

    fun calculateWorkTimeTodayUpdate(
        end: LocalTime,
        dailyWorkTime: LocalTime,
        workTimeToday: LocalTime,
        oldWorkTimeToday: LocalTime
    ): TimeUpdateResult {
        val newEnd = if (oldWorkTimeToday == LocalTime.MIDNIGHT) {
            end.subtract(dailyWorkTime).add(workTimeToday)
        } else {
            end.subtract(oldWorkTimeToday).add(workTimeToday)
        }
        return TimeUpdateResult(endTime = newEnd.toString())
    }

    private fun LocalTime.subtract(time: LocalTime): LocalTime {
        return minusHours(time.hour.toLong()).minusMinutes(time.minute.toLong())
    }

    private fun LocalTime.add(time: LocalTime): LocalTime {
        return plusHours(time.hour.toLong()).plusMinutes(time.minute.toLong())
    }

    private fun timeToMinutes(time: String): Int {
        val colonIndex = time.indexOf(':')
        if (colonIndex == -1) return 0
        val hours = time.substring(0, colonIndex).toInt()
        val minutes = time.substring(colonIndex + 1).toInt()
        return hours * MINUTES_IN_HOUR + minutes
    }

    private fun calculateHalfTime(start: LocalTime, dailyWorkTime: LocalTime): LocalTime {
        val totalMinutes = (dailyWorkTime.hour * MINUTES_IN_HOUR + dailyWorkTime.minute) / 2
        return start.plusMinutes(totalMinutes.toLong())
    }

    fun checkIfDoubleMinus(value: String): String {
        return if (value.startsWith("--")) value.substring(2) else value
    }

    data class TimeUpdateResult(
        val endTime: String? = null,
        val lunchStart: String? = null,
        val lunchEnd: String? = null,
        val breakStart: String? = null,
        val breakEnd: String? = null,
        val workTimeToday: String? = null,
        val calculateBalance: Boolean = false
    )

    data class StartTimeUpdateParams(
        val start: LocalTime,
        val dailyWorkTime: LocalTime,
        val lunchTime: LocalTime,
        val workTimeToday: LocalTime,
        val oldStartTime: LocalTime,
        val isNewDay: Boolean
    )

    data class EndTimeUpdateParams(
        val start: LocalTime,
        val end: LocalTime,
        val lunchStart: LocalTime,
        val lunchEnd: LocalTime,
        val breakStart: LocalTime,
        val breakEnd: LocalTime,
        val workTimeToday: LocalTime,
        val oldEndTime: LocalTime
    )
}

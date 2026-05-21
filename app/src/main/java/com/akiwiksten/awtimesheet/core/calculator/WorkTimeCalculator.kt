package com.akiwiksten.awtimesheet.core.calculator

import com.akiwiksten.awtimesheet.core.MINUTES_IN_HOUR
import com.akiwiksten.awtimesheet.core.TIME_FORMAT
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/**
 * Pure logic for time calculations and flex time updates.
 * This class is stateless and provides calculation results back to the caller.
 */
object WorkTimeCalculator {

    private val timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
    private val dayFormatter = DateTimeFormatter.ofPattern("dd")

    /**
     * Returns day-of-month (`dd`) extracted from [workday].
     *
     * Expected [workday] format: `yyyy-MM-dd` (for example `2026-04-13`).
     * For this exact ISO format, the function uses substring extraction for performance.
     */
    fun extractDayOfMonth(workday: String): String {
        // Optimization: LocalDate.parse is relatively slow.
        // If workday is always in ISO format (yyyy-MM-dd), we can just take the last 2 chars.
        return if (workday.length >= 10 && workday[4] == '-' && workday[7] == '-') {
            workday.substring(8, 10)
        } else {
            val formattedDate = LocalDate.parse(workday)
            dayFormatter.format(formattedDate)
        }
    }

    fun stringToLocalTime(time: String): LocalTime {
        return if (time.isEmpty()) {
            LocalTime.parse(ZERO_TIME, timeFormatter)
        } else {
            LocalTime.parse(time, timeFormatter)
        }
    }

    fun calculateFlexTime(initialTime: String, addedTime: String): String {
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

    private fun timeToMinutes(time: String): Int {
        val colonIndex = time.indexOf(':')
        if (colonIndex == -1) return 0
        val hours = time.substring(0, colonIndex).toInt()
        val minutes = time.substring(colonIndex + 1).toInt()
        return hours * MINUTES_IN_HOUR + minutes
    }

    fun normalizeDuplicateMinus(value: String): String {
        return if (value.startsWith("--")) value.substring(2) else value
    }

    data class TimeUpdateResult(
        val end: String? = null,
        val lunchStart: String? = null,
        val lunchEnd: String? = null,
        val breakStart: String? = null,
        val breakEnd: String? = null,
        val projectTime: String? = null,
        val shouldRecalculateFlexTime: Boolean = false
    )

    data class StartTimeUpdateParams(
        val start: LocalTime,
        val dailyWorkTimeEstimate: LocalTime,
        val dailyLunchTimeEstimate: LocalTime,
        val projectTime: LocalTime,
        val oldStartTime: LocalTime,
        val isNewDayForProject: Boolean
    )

    data class EndTimeUpdateParams(
        val start: LocalTime,
        val end: LocalTime,
        val lunchStart: LocalTime,
        val lunchEnd: LocalTime,
        val breakStart: LocalTime,
        val breakEnd: LocalTime,
        val projectTime: LocalTime,
        val oldEndTime: LocalTime
    )
}

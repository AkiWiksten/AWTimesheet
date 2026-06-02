package com.akiwiksten.awtimesheet.feature.projectdetails.calculator

import com.akiwiksten.awtimesheet.core.MINUTES_IN_HOUR
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import java.time.LocalTime

/**
 * Calculates dependent time-field updates for Project Details form edits.
 */
internal object ProjectDetailsTimeUpdateCalculator {
    fun calculateStartTimeUpdate(
        params: WorkTimeCalculator.StartTimeUpdateParams
    ): WorkTimeCalculator.TimeUpdateResult {
        return if (params.isNewDayForProject) {
            val end = params.start.add(params.dailyWorkTimeEstimate).add(params.dailyLunchTimeEstimate)
            val lunchStart = calculateHalfTime(params.start, params.dailyWorkTimeEstimate)
            WorkTimeCalculator.TimeUpdateResult(
                end = end.toString(),
                lunchStart = lunchStart.toString(),
                lunchEnd = lunchStart.add(params.dailyLunchTimeEstimate).toString(),
                breakStart = params.start.toString(),
                breakEnd = params.start.toString()
            )
        } else if (params.projectTime != LocalTime.MIDNIGHT) {
            val newProjectTime = params.projectTime.subtract(params.start).add(params.oldStartTime)
            WorkTimeCalculator.TimeUpdateResult(projectTime = newProjectTime.toString())
        } else {
            WorkTimeCalculator.TimeUpdateResult()
        }
    }

    fun calculateEndTimeUpdate(params: WorkTimeCalculator.EndTimeUpdateParams): WorkTimeCalculator.TimeUpdateResult {
        val newProjectTime = if (params.projectTime == LocalTime.MIDNIGHT) {
            params.end.subtract(params.start).subtract(params.lunchEnd).add(params.lunchStart)
                .subtract(params.breakEnd).add(params.breakStart)
        } else {
            params.projectTime.subtract(params.oldEndTime).add(params.end)
        }
        return WorkTimeCalculator.TimeUpdateResult(projectTime = newProjectTime.toString())
    }

    fun calculateDailyWorkTimeUpdate(
        end: LocalTime,
        dailyWorkTimeEstimate: LocalTime,
        projectTime: LocalTime,
        oldDailyWorkTimeEstimate: LocalTime,
        isNewDayForProject: Boolean
    ): WorkTimeCalculator.TimeUpdateResult {
        return if (!isNewDayForProject && projectTime == LocalTime.MIDNIGHT) {
            val newEnd = end.subtract(oldDailyWorkTimeEstimate).add(dailyWorkTimeEstimate)
            WorkTimeCalculator.TimeUpdateResult(end = newEnd.toString())
        } else {
            WorkTimeCalculator.TimeUpdateResult()
        }
    }

    fun calculateLunchStartUpdate(
        lunchStart: LocalTime,
        dailyLunchTimeEstimate: LocalTime,
        projectTime: LocalTime,
        oldLunchStart: LocalTime,
        currentLunchEnd: LocalTime
    ): WorkTimeCalculator.TimeUpdateResult {
        val newLunchEnd = if (projectTime == LocalTime.MIDNIGHT) {
            lunchStart.add(dailyLunchTimeEstimate)
        } else {
            currentLunchEnd.subtract(oldLunchStart).add(lunchStart)
        }
        return WorkTimeCalculator.TimeUpdateResult(lunchEnd = newLunchEnd.toString())
    }

    fun calculateLunchEndUpdate(
        end: LocalTime,
        lunchEnd: LocalTime,
        projectTime: LocalTime,
        oldLunchEnd: LocalTime
    ): WorkTimeCalculator.TimeUpdateResult {
        return if (projectTime == LocalTime.MIDNIGHT) {
            val newEnd = end.subtract(oldLunchEnd).add(lunchEnd)
            WorkTimeCalculator.TimeUpdateResult(end = newEnd.toString())
        } else {
            val newProjectTime = projectTime.subtract(lunchEnd).add(oldLunchEnd)
            WorkTimeCalculator.TimeUpdateResult(
                projectTime = newProjectTime.toString(),
                shouldRecalculateFlexTime = true
            )
        }
    }

    fun calculateLunchTimeUpdate(
        end: LocalTime,
        lunchStart: LocalTime,
        dailyLunchTimeEstimate: LocalTime,
        projectTime: LocalTime,
        oldDailyLunchTimeEstimate: LocalTime
    ): WorkTimeCalculator.TimeUpdateResult {
        return if (projectTime == LocalTime.MIDNIGHT) {
            val newEnd = end.add(dailyLunchTimeEstimate).subtract(oldDailyLunchTimeEstimate)
            val newLunchEnd = lunchStart.add(dailyLunchTimeEstimate)
            WorkTimeCalculator.TimeUpdateResult(end = newEnd.toString(), lunchEnd = newLunchEnd.toString())
        } else {
            WorkTimeCalculator.TimeUpdateResult()
        }
    }

    fun calculateBreakStartUpdate(
        end: LocalTime,
        breakStart: LocalTime,
        breakEnd: LocalTime,
        projectTime: LocalTime,
        oldBreakStart: LocalTime
    ): WorkTimeCalculator.TimeUpdateResult {
        return when {
            oldBreakStart == breakEnd -> WorkTimeCalculator.TimeUpdateResult(breakEnd = breakStart.toString())
            projectTime == LocalTime.MIDNIGHT -> {
                val newEnd = end.subtract(oldBreakStart).add(breakEnd)
                WorkTimeCalculator.TimeUpdateResult(end = newEnd.toString())
            }
            else -> {
                val newProjectTime = projectTime.subtract(oldBreakStart).add(breakStart)
                WorkTimeCalculator.TimeUpdateResult(
                    projectTime = newProjectTime.toString(),
                    shouldRecalculateFlexTime = true
                )
            }
        }
    }

    fun calculateBreakEndUpdate(
        end: LocalTime,
        projectTime: LocalTime,
        breakEnd: LocalTime,
        oldBreakEnd: LocalTime
    ): WorkTimeCalculator.TimeUpdateResult {
        return if (projectTime == LocalTime.MIDNIGHT) {
            val newEnd = end.subtract(oldBreakEnd).add(breakEnd)
            WorkTimeCalculator.TimeUpdateResult(end = newEnd.toString())
        } else {
            val newProjectTime = projectTime.subtract(breakEnd).add(oldBreakEnd)
            WorkTimeCalculator.TimeUpdateResult(
                projectTime = newProjectTime.toString(),
                shouldRecalculateFlexTime = true
            )
        }
    }

    fun calculateProjectTimeUpdate(
        end: LocalTime,
        dailyWorkTimeEstimate: LocalTime,
        projectTime: LocalTime,
        oldProjectTime: LocalTime
    ): WorkTimeCalculator.TimeUpdateResult {
        val newEnd = if (oldProjectTime == LocalTime.MIDNIGHT) {
            end.subtract(dailyWorkTimeEstimate).add(projectTime)
        } else {
            end.subtract(oldProjectTime).add(projectTime)
        }
        return WorkTimeCalculator.TimeUpdateResult(end = newEnd.toString())
    }

    fun calculateProjectTimeUpdate(
        projectTime: LocalTime,
        dailyLunchTimeEstimate: LocalTime
    ): WorkTimeCalculator.TimeUpdateResult {
        val end = LocalTime.MIDNIGHT
        val newEnd = end.add(projectTime).add(dailyLunchTimeEstimate)
        return WorkTimeCalculator.TimeUpdateResult(end = newEnd.toString())
    }
}

private fun calculateHalfTime(start: LocalTime, dailyWorkTimeEstimate: LocalTime): LocalTime {
    val totalMinutes = (dailyWorkTimeEstimate.hour * MINUTES_IN_HOUR + dailyWorkTimeEstimate.minute) / 2
    return start.plusMinutes(totalMinutes.toLong())
}

private fun LocalTime.subtract(time: LocalTime): LocalTime {
    return minusHours(time.hour.toLong()).minusMinutes(time.minute.toLong())
}

private fun LocalTime.add(time: LocalTime): LocalTime {
    return plusHours(time.hour.toLong()).plusMinutes(time.minute.toLong())
}

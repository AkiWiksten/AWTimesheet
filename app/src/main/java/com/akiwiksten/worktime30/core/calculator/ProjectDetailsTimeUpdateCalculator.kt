package com.akiwiksten.worktime30.core.calculator

import com.akiwiksten.worktime30.core.MINUTES_IN_HOUR
import java.time.LocalTime

/**
 * Calculates dependent time-field updates for Project Details form edits.
 */
object ProjectDetailsTimeUpdateCalculator {
    fun calculateStartTimeUpdate(
        params: WorkTimeCalculator.StartTimeUpdateParams
    ): WorkTimeCalculator.TimeUpdateResult {
        return if (params.isNewDay) {
            val end = params.start.add(params.dailyWorkTime).add(params.lunchTime)
            val lunchStart = calculateHalfTime(params.start, params.dailyWorkTime)
            WorkTimeCalculator.TimeUpdateResult(
                end = end.toString(),
                lunchStart = lunchStart.toString(),
                lunchEnd = lunchStart.add(params.lunchTime).toString(),
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
        dailyWorkTime: LocalTime,
        projectTime: LocalTime,
        oldDailyWorkTime: LocalTime,
        isNewDay: Boolean
    ): WorkTimeCalculator.TimeUpdateResult {
        return if (!isNewDay && projectTime == LocalTime.MIDNIGHT) {
            val newEnd = end.subtract(oldDailyWorkTime).add(dailyWorkTime)
            WorkTimeCalculator.TimeUpdateResult(end = newEnd.toString())
        } else {
            WorkTimeCalculator.TimeUpdateResult()
        }
    }

    fun calculateLunchStartUpdate(
        lunchStart: LocalTime,
        lunchTime: LocalTime,
        projectTime: LocalTime,
        oldLunchStart: LocalTime,
        currentLunchEnd: LocalTime
    ): WorkTimeCalculator.TimeUpdateResult {
        val newLunchEnd = if (projectTime == LocalTime.MIDNIGHT) {
            lunchStart.add(lunchTime)
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
        lunchTime: LocalTime,
        projectTime: LocalTime,
        oldLunchTime: LocalTime
    ): WorkTimeCalculator.TimeUpdateResult {
        return if (projectTime == LocalTime.MIDNIGHT) {
            val newEnd = end.add(lunchTime).subtract(oldLunchTime)
            val newLunchEnd = lunchStart.add(lunchTime)
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
        dailyWorkTime: LocalTime,
        projectTime: LocalTime,
        oldProjectTime: LocalTime
    ): WorkTimeCalculator.TimeUpdateResult {
        val newEnd = if (oldProjectTime == LocalTime.MIDNIGHT) {
            end.subtract(dailyWorkTime).add(projectTime)
        } else {
            end.subtract(oldProjectTime).add(projectTime)
        }
        return WorkTimeCalculator.TimeUpdateResult(end = newEnd.toString())
    }
}

private fun calculateHalfTime(start: LocalTime, dailyWorkTime: LocalTime): LocalTime {
    val totalMinutes = (dailyWorkTime.hour * MINUTES_IN_HOUR + dailyWorkTime.minute) / 2
    return start.plusMinutes(totalMinutes.toLong())
}

private fun LocalTime.subtract(time: LocalTime): LocalTime {
    return minusHours(time.hour.toLong()).minusMinutes(time.minute.toLong())
}

private fun LocalTime.add(time: LocalTime): LocalTime {
    return plusHours(time.hour.toLong()).plusMinutes(time.minute.toLong())
}

package com.akiwiksten.awtimesheet.feature.projectdetails.model

import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator.EndTimeUpdateParams
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator.StartTimeUpdateParams
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.hasOnlyProjectTime
import com.akiwiksten.awtimesheet.domain.model.isNewDayForProject
import com.akiwiksten.awtimesheet.feature.projectdetails.calculator.ProjectDetailsTimeUpdateCalculator

internal fun ProjectDetailsState.updateTimeField(
    field: ProjectDetailsField,
    time: String,
    settings: SettingsState
): ProjectDetailsState {
    val isNewDay = isNewDayForProject()
    val currentLunchEstimate = if (isNewDay) settings.dailyLunchTimeEstimate else lunchTimeEstimate

    if (field == ProjectDetailsField.PROJECT_TIME) {
        val nextDetails = copy(projectTime = time)
        if (nextDetails.hasOnlyProjectTime()) {
            return ProjectDetailsUiMapper.normalizeProjectDetails(
                nextDetails,
                settings
            ) ?: nextDetails
        }
    }

    val update = calculateUpdate(field, time, settings, currentLunchEstimate, isNewDay)
    val baseDetails = updateBaseFields(field, time, currentLunchEstimate, isNewDay)

    return baseDetails.applyUpdate(update)
}

private fun ProjectDetailsState.calculateUpdate(
    field: ProjectDetailsField,
    time: String,
    settings: SettingsState,
    currentLunchEstimate: String,
    isNewDay: Boolean
): WorkTimeCalculator.TimeUpdateResult {
    return when (field) {
        ProjectDetailsField.START_TIME -> {
            ProjectDetailsTimeUpdateCalculator.calculateStartTimeUpdate(
                StartTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(time),
                    dailyWorkTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                        settings.dailyWorkTimeEstimate
                    ),
                    dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                        currentLunchEstimate
                    ),
                    projectTime = WorkTimeCalculator.stringToLocalTime(projectTime),
                    oldStartTime = WorkTimeCalculator.stringToLocalTime(startTime),
                    isNewDayForProject = isNewDay
                )
            )
        }

        ProjectDetailsField.END_TIME -> {
            ProjectDetailsTimeUpdateCalculator.calculateEndTimeUpdate(
                EndTimeUpdateParams(
                    start = WorkTimeCalculator.stringToLocalTime(startTime),
                    end = WorkTimeCalculator.stringToLocalTime(time),
                    lunchStart = WorkTimeCalculator.stringToLocalTime(lunchStart),
                    lunchEnd = WorkTimeCalculator.stringToLocalTime(lunchEnd),
                    breakStart = WorkTimeCalculator.stringToLocalTime(breakStart),
                    breakEnd = WorkTimeCalculator.stringToLocalTime(breakEnd),
                    projectTime = WorkTimeCalculator.stringToLocalTime(projectTime),
                    oldEndTime = WorkTimeCalculator.stringToLocalTime(endTime)
                )
            )
        }

        ProjectDetailsField.LUNCH_START -> {
            ProjectDetailsTimeUpdateCalculator.calculateLunchStartUpdate(
                lunchStart = WorkTimeCalculator.stringToLocalTime(time),
                dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                    currentLunchEstimate
                ),
                projectTime = WorkTimeCalculator.stringToLocalTime(projectTime),
                oldLunchStart = WorkTimeCalculator.stringToLocalTime(lunchStart),
                currentLunchEnd = WorkTimeCalculator.stringToLocalTime(lunchEnd)
            )
        }

        else -> calculateRemainingUpdates(field, time, settings, currentLunchEstimate)
    }
}

private fun ProjectDetailsState.calculateRemainingUpdates(
    field: ProjectDetailsField,
    time: String,
    settings: SettingsState,
    currentLunchEstimate: String
): WorkTimeCalculator.TimeUpdateResult {
    return when (field) {
        ProjectDetailsField.LUNCH_END -> {
            ProjectDetailsTimeUpdateCalculator.calculateLunchEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(endTime),
                lunchEnd = WorkTimeCalculator.stringToLocalTime(time),
                projectTime = WorkTimeCalculator.stringToLocalTime(projectTime),
                oldLunchEnd = WorkTimeCalculator.stringToLocalTime(lunchEnd)
            )
        }

        ProjectDetailsField.LUNCH_TIME -> {
            ProjectDetailsTimeUpdateCalculator.calculateLunchTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(endTime),
                lunchStart = WorkTimeCalculator.stringToLocalTime(lunchStart),
                dailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(time),
                projectTime = WorkTimeCalculator.stringToLocalTime(projectTime),
                oldDailyLunchTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                    currentLunchEstimate
                )
            )
        }

        ProjectDetailsField.BREAK_START -> {
            ProjectDetailsTimeUpdateCalculator.calculateBreakStartUpdate(
                end = WorkTimeCalculator.stringToLocalTime(endTime),
                breakStart = WorkTimeCalculator.stringToLocalTime(time),
                breakEnd = WorkTimeCalculator.stringToLocalTime(breakEnd),
                projectTime = WorkTimeCalculator.stringToLocalTime(projectTime),
                oldBreakStart = WorkTimeCalculator.stringToLocalTime(breakStart)
            )
        }

        ProjectDetailsField.BREAK_END -> {
            ProjectDetailsTimeUpdateCalculator.calculateBreakEndUpdate(
                end = WorkTimeCalculator.stringToLocalTime(endTime),
                projectTime = WorkTimeCalculator.stringToLocalTime(projectTime),
                breakEnd = WorkTimeCalculator.stringToLocalTime(time),
                oldBreakEnd = WorkTimeCalculator.stringToLocalTime(breakEnd)
            )
        }

        ProjectDetailsField.PROJECT_TIME -> {
            ProjectDetailsTimeUpdateCalculator.calculateProjectTimeUpdate(
                end = WorkTimeCalculator.stringToLocalTime(endTime),
                dailyWorkTimeEstimate = WorkTimeCalculator.stringToLocalTime(
                    settings.dailyWorkTimeEstimate
                ),
                projectTime = WorkTimeCalculator.stringToLocalTime(time),
                oldProjectTime = WorkTimeCalculator.stringToLocalTime(projectTime)
            )
        }

        else -> WorkTimeCalculator.TimeUpdateResult()
    }
}

private fun ProjectDetailsState.updateBaseFields(
    field: ProjectDetailsField,
    time: String,
    currentLunchEstimate: String,
    isNewDay: Boolean
): ProjectDetailsState {
    return when (field) {
        ProjectDetailsField.START_TIME -> {
            if (isNewDay) {
                copy(startTime = time, lunchTimeEstimate = currentLunchEstimate)
            } else {
                copy(startTime = time)
            }
        }

        ProjectDetailsField.END_TIME -> copy(endTime = time)
        ProjectDetailsField.LUNCH_START -> copy(lunchStart = time)
        ProjectDetailsField.LUNCH_END -> copy(lunchEnd = time)
        ProjectDetailsField.LUNCH_TIME -> copy(lunchTimeEstimate = time)
        ProjectDetailsField.BREAK_START -> copy(breakStart = time)
        ProjectDetailsField.BREAK_END -> copy(breakEnd = time)
        ProjectDetailsField.PROJECT_TIME -> copy(projectTime = time)
    }
}

private fun ProjectDetailsState.applyUpdate(
    result: WorkTimeCalculator.TimeUpdateResult
): ProjectDetailsState {
    return copy(
        endTime = result.end ?: endTime,
        lunchStart = result.lunchStart ?: lunchStart,
        lunchEnd = result.lunchEnd ?: lunchEnd,
        breakStart = result.breakStart ?: breakStart,
        breakEnd = result.breakEnd ?: breakEnd,
        projectTime = result.projectTime ?: projectTime
    )
}

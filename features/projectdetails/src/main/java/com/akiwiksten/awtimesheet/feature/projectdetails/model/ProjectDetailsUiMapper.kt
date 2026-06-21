package com.akiwiksten.awtimesheet.feature.projectdetails.model

import com.akiwiksten.awtimesheet.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.awtimesheet.core.TIME_FORMAT
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.hasOnlyProjectTime
import com.akiwiksten.awtimesheet.domain.model.isNewDayForProject
import com.akiwiksten.awtimesheet.feature.projectdetails.ProjectDetailsUiState
import com.akiwiksten.awtimesheet.feature.projectdetails.calculator.ProjectDetailsTimeUpdateCalculator
import java.time.format.DateTimeFormatter

object ProjectDetailsUiMapper {
    private val timeFormatter = DateTimeFormatter.ofPattern(TIME_FORMAT)

    fun mapEntitiesToUiState(
        baseState: ProjectDetailsUiState.Success,
        projectDetails: ProjectDetailsState?,
        settings: SettingsState?
    ): ProjectDetailsUiState.Success {
        val normalizedSettings = normalizedSettings(settings = settings)
        val mappedData = projectDetails?.let {
            applyProjectDetails(
                baseState = baseState.details,
                projectDetails = it,
                defaultLunchTimeEstimate = normalizedSettings.dailyLunchTimeEstimate
            )
        } ?: applyEmptyDayDefaults(
            state = baseState.details,
            lunchTimeEstimate = normalizedSettings.dailyLunchTimeEstimate
        )

        return baseState.copy(details = mappedData, settings = normalizedSettings)
    }

    private fun normalizedSettings(settings: SettingsState?): SettingsState {
        val data = settings ?: SettingsState()
        return SettingsState(
            dailyWorkTimeEstimate = data.dailyWorkTimeEstimate.ifEmpty { DEFAULT_DAILY_WORK_TIME },
            dailyLunchTimeEstimate = data.dailyLunchTimeEstimate.ifEmpty { ZERO_TIME },
            initialFlexTimeTotal = data.initialFlexTimeTotal.ifEmpty { ZERO_TIME }
        )
    }

    private fun applyProjectDetails(
        baseState: ProjectDetailsState,
        projectDetails: ProjectDetailsState,
        defaultLunchTimeEstimate: String
    ): ProjectDetailsState {
        val resolvedLunchTimeEstimate = if (projectDetails.isNewDayForProject()) {
            defaultLunchTimeEstimate
        } else {
            projectDetails.lunchTimeEstimate.ifEmpty { ZERO_TIME }
        }

        return baseState.copy(
            date = projectDetails.date,
            projectName = baseState.projectName,
            startTime = projectDetails.startTime.ifEmpty { ZERO_TIME },
            endTime = projectDetails.endTime.ifEmpty { ZERO_TIME },
            lunchStart = projectDetails.lunchStart.ifEmpty { ZERO_TIME },
            lunchEnd = projectDetails.lunchEnd.ifEmpty { ZERO_TIME },
            breakStart = projectDetails.breakStart.ifEmpty { ZERO_TIME },
            breakEnd = projectDetails.breakEnd.ifEmpty { ZERO_TIME },
            projectTime = baseState.projectTime,
            lunchTimeEstimate = resolvedLunchTimeEstimate
        )
    }

    private fun applyEmptyDayDefaults(state: ProjectDetailsState, lunchTimeEstimate: String): ProjectDetailsState {
        return state.copy(
            startTime = ZERO_TIME,
            endTime = ZERO_TIME,
            lunchStart = ZERO_TIME,
            lunchEnd = ZERO_TIME,
            breakStart = ZERO_TIME,
            breakEnd = ZERO_TIME,
            projectTime = state.projectTime,
            lunchTimeEstimate = lunchTimeEstimate
        )
    }

    fun normalizeProjectDetails(
        projectDetails: ProjectDetailsState?,
        settings: SettingsState?
    ): ProjectDetailsState? {
        if (projectDetails == null || !projectDetails.hasOnlyProjectTime()) {
            return projectDetails
        }

        val update = ProjectDetailsTimeUpdateCalculator.calculateProjectTimeUpdate(
            projectTime = WorkTimeCalculator.stringToLocalTime(projectDetails.projectTime),
            dailyLunchTimeEstimate = WorkTimeCalculator
                .stringToLocalTime(settings?.dailyLunchTimeEstimate ?: ZERO_TIME)
        )
        return projectDetails.copy(
            startTime = ZERO_TIME,
            endTime = update.end?.format(timeFormatter) ?: ZERO_TIME,
            lunchStart = update.lunchStart?.format(timeFormatter) ?: ZERO_TIME,
            lunchEnd = update.lunchEnd?.format(timeFormatter) ?: ZERO_TIME,
            breakStart = update.breakStart?.format(timeFormatter) ?: ZERO_TIME,
            breakEnd = update.breakEnd?.format(timeFormatter) ?: ZERO_TIME
        )
    }
}

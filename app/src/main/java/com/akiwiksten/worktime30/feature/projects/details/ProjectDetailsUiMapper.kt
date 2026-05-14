package com.akiwiksten.worktime30.feature.projects.details

import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.isNewDayForProject

object ProjectDetailsUiMapper {
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
            projectName = projectDetails.projectName,
            startTime = projectDetails.startTime.ifEmpty { ZERO_TIME },
            endTime = projectDetails.endTime.ifEmpty { ZERO_TIME },
            lunchStart = projectDetails.lunchStart.ifEmpty { ZERO_TIME },
            lunchEnd = projectDetails.lunchEnd.ifEmpty { ZERO_TIME },
            breakStart = projectDetails.breakStart.ifEmpty { ZERO_TIME },
            breakEnd = projectDetails.breakEnd.ifEmpty { ZERO_TIME },
            projectTime = projectDetails.projectTime.ifEmpty { ZERO_TIME },
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
            projectTime = ZERO_TIME,
            lunchTimeEstimate = lunchTimeEstimate
        )
    }

    fun normalizeProjectTimeOnOpen(startTime: String, endTime: String, projectTime: String): String {
        return if (startTime == ZERO_TIME && endTime == ZERO_TIME && projectTime != ZERO_TIME) {
            ZERO_TIME
        } else {
            projectTime
        }
    }
}

package com.akiwiksten.worktime30.feature.projects.details

import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.WorkStatsState

object ProjectDetailsUiMapper {
    fun applyEntitiesToState(
        baseState: ProjectDetailsUiState.Success,
        projectDetails: ProjectDetailsState?,
        workStats: WorkStatsState?
    ): ProjectDetailsUiState.Success {
        val stateWithWorkStats = baseState.data.copy(workStats = normalizedWorkStats(workStats = workStats))
        val mappedData = projectDetails?.let {
            applyProjectDetails(state = stateWithWorkStats, projectDetails = it)
        } ?: applyEmptyDayDefaults(state = stateWithWorkStats)

        return baseState.copy(data = mappedData)
    }

    private fun normalizedWorkStats(workStats: WorkStatsState?): WorkStatsState {
        val data = workStats ?: WorkStatsState()
        return WorkStatsState(
            dailyWorkTimeEstimate = data.dailyWorkTimeEstimate.ifEmpty { DEFAULT_DAILY_WORK_TIME },
            dailyLunchTimeEstimate = data.dailyLunchTimeEstimate.ifEmpty { ZERO_TIME },
            initialFlexTimeTotal = data.initialFlexTimeTotal.ifEmpty { ZERO_TIME }
        )
    }

    private fun applyProjectDetails(
        state: ProjectDetailsState,
        projectDetails: ProjectDetailsState
    ): ProjectDetailsState {
        val normalizedStartTime = projectDetails.startTime.ifEmpty { ZERO_TIME }
        val normalizedEndTime = projectDetails.endTime.ifEmpty { ZERO_TIME }
        val normalizedProjectTime = normalizeProjectTimeOnOpen(
            startTime = normalizedStartTime,
            endTime = normalizedEndTime,
            projectTime = projectDetails.projectTime.ifEmpty { ZERO_TIME }
        )

        return state.copy(
            date = projectDetails.date,
            projectName = projectDetails.projectName,
            startTime = normalizedStartTime,
            endTime = normalizedEndTime,
            lunchStart = projectDetails.lunchStart.ifEmpty { ZERO_TIME },
            lunchEnd = projectDetails.lunchEnd.ifEmpty { ZERO_TIME },
            breakStart = projectDetails.breakStart.ifEmpty { ZERO_TIME },
            breakEnd = projectDetails.breakEnd.ifEmpty { ZERO_TIME },
            projectTime = normalizedProjectTime
        )
    }

    private fun applyEmptyDayDefaults(state: ProjectDetailsState): ProjectDetailsState {
        return state.copy(
            startTime = ZERO_TIME,
            endTime = ZERO_TIME,
            lunchStart = ZERO_TIME,
            lunchEnd = ZERO_TIME,
            breakStart = ZERO_TIME,
            breakEnd = ZERO_TIME,
            projectTime = ZERO_TIME
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

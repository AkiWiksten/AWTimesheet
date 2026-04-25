package com.akiwiksten.worktime30.feature.projects.details

import com.akiwiksten.worktime30.core.ZERO_TIME

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
            dailyWorkTime = data.dailyWorkTime.ifEmpty { "07:30" },
            lunchTime = data.lunchTime.ifEmpty { ZERO_TIME },
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
        val normalizedProjectDetails = projectDetails.copy(
            startTime = normalizedStartTime,
            endTime = normalizedEndTime,
            projectTime = normalizedProjectTime
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
            projectTime = normalizedProjectTime,
            flexTimeToday = projectDetails.flexTimeToday.ifEmpty { ZERO_TIME },
            oldFlexTimeToday = projectDetails.flexTimeToday.ifEmpty { ZERO_TIME },
            isNewDay = isNewDay(normalizedProjectDetails)
        )
    }

    private fun applyEmptyDayDefaults(state: ProjectDetailsState): ProjectDetailsState {
        return state.copy(
            isNewDay = true,
            startTime = ZERO_TIME,
            endTime = ZERO_TIME,
            lunchStart = ZERO_TIME,
            lunchEnd = ZERO_TIME,
            breakStart = ZERO_TIME,
            breakEnd = ZERO_TIME,
            projectTime = ZERO_TIME,
            flexTimeToday = ZERO_TIME,
            oldFlexTimeToday = ZERO_TIME
        )
    }

    fun normalizeProjectTimeOnOpen(startTime: String, endTime: String, projectTime: String): String {
        return if (startTime == ZERO_TIME && endTime == ZERO_TIME && projectTime != ZERO_TIME) {
            ZERO_TIME
        } else {
            projectTime
        }
    }

    private fun isNewDay(projectDetails: ProjectDetailsState): Boolean {
        fun isZero(time: String) = time == ZERO_TIME || time.isEmpty()
        return isZero(projectDetails.startTime) &&
            isZero(projectDetails.endTime) &&
            isZero(projectDetails.lunchEnd) &&
            isZero(projectDetails.lunchStart) &&
            isZero(projectDetails.projectTime) &&
            isZero(projectDetails.breakStart) &&
            isZero(projectDetails.breakEnd)
    }
}

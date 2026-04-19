package com.akiwiksten.worktime30.feature.projects.single.details

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity

object ProjectDetailsUiMapper {

    fun mapToEntity(state: ProjectDetailsState): ProjectDetailsEntity {
        return ProjectDetailsEntity(
            date = state.date,
            projectName = state.projectName,
            startTime = state.startTime,
            endTime = state.endTime,
            lunchStart = state.lunchStart,
            lunchEnd = state.lunchEnd,
            breakStart = state.breakStart,
            breakEnd = state.breakEnd,
            projectTime = state.projectTime,
            balanceToday = state.balanceToday,
        )
    }

    fun mapToWorkStatsEntity(state: ProjectDetailsState): WorkStatsEntity {
        return WorkStatsEntity(
            dailyWorkTime = state.workStats.dailyWorkTime,
            lunchTime = state.workStats.lunchTime,
            workTimeTotal = state.workStats.workTimeTotal,
            balanceTotal = state.workStats.balanceTotal
        )
    }

    fun applyEntitiesToState(
        baseState: ProjectDetailsUiState.Success,
        projectDetails: ProjectDetailsState?,
        workStats: WorkStatsState?
    ): ProjectDetailsUiState.Success {
        var state = baseState.data
        state = if (workStats != null) {
            state.copy(
                workStats = WorkStatsState(
                    dailyWorkTime = workStats.dailyWorkTime.ifEmpty { "07:30" },
                    lunchTime = workStats.lunchTime.ifEmpty { ZERO_TIME },
                    workTimeTotal = workStats.workTimeTotal.ifEmpty { ZERO_TIME },
                    balanceTotal = workStats.balanceTotal.ifEmpty { ZERO_TIME }
                )
            )
        } else {
            state.copy(
                workStats = WorkStatsState(
                    dailyWorkTime = "07:30",
                    lunchTime = ZERO_TIME,
                    workTimeTotal = ZERO_TIME,
                    balanceTotal = ZERO_TIME
                )
            )
        }

        return if (projectDetails != null) {
            baseState.copy(
                data = state.copy(
                    date = projectDetails.date,
                    projectName = projectDetails.projectName,
                    startTime = projectDetails.startTime.ifEmpty { ZERO_TIME },
                    endTime = projectDetails.endTime.ifEmpty { ZERO_TIME },
                    lunchStart = projectDetails.lunchStart.ifEmpty { ZERO_TIME },
                    lunchEnd = projectDetails.lunchEnd.ifEmpty { ZERO_TIME },
                    breakStart = projectDetails.breakStart.ifEmpty { ZERO_TIME },
                    breakEnd = projectDetails.breakEnd.ifEmpty { ZERO_TIME },
                    projectTime = projectDetails.projectTime.ifEmpty { ZERO_TIME },
                    balanceToday = projectDetails.balanceToday.ifEmpty { ZERO_TIME },
                    oldBalanceToday = projectDetails.balanceToday.ifEmpty { ZERO_TIME },
                    isNewDay = isNewDay(projectDetails)
                )
            )
        } else {
            baseState.copy(
                data = state.copy(
                    isNewDay = true,
                    startTime = ZERO_TIME,
                    endTime = ZERO_TIME,
                    lunchStart = ZERO_TIME,
                    lunchEnd = ZERO_TIME,
                    breakStart = ZERO_TIME,
                    breakEnd = ZERO_TIME,
                    projectTime = ZERO_TIME,
                    balanceToday = ZERO_TIME,
                    oldBalanceToday = ZERO_TIME
                )
            )
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

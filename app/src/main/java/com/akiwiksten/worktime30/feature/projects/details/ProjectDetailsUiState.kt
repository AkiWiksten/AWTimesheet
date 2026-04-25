package com.akiwiksten.worktime30.feature.projects.details

import com.akiwiksten.worktime30.core.ZERO_TIME

sealed class ProjectDetailsUiState {
    object Loading : ProjectDetailsUiState()

    data class Success(
        val data: ProjectDetailsState
    ) : ProjectDetailsUiState()

    data class Error(val message: String) : ProjectDetailsUiState()
}

data class WorkStatsState(
    val dailyWorkTime: String = ZERO_TIME,
    val lunchTime: String = ZERO_TIME,
    val initialFlexTimeTotal: String = ZERO_TIME
)

data class ProjectDetailsState(
    val date: String = "",
    val projectName: String = "",
    val startTime: String = ZERO_TIME,
    val endTime: String = ZERO_TIME,
    val lunchStart: String = ZERO_TIME,
    val lunchEnd: String = ZERO_TIME,
    val breakStart: String = ZERO_TIME,
    val breakEnd: String = ZERO_TIME,
    val projectTime: String = ZERO_TIME,
    val otherProjectsTotalTime: String = ZERO_TIME,
    val hasOtherProjects: Boolean = false,
    val flexTimeToday: String = ZERO_TIME,
    val oldFlexTimeToday: String = ZERO_TIME,
    val isNewDay: Boolean = true,
    val workStats: WorkStatsState = WorkStatsState()
)

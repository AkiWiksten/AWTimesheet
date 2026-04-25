package com.akiwiksten.worktime30.feature.projects.single

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.feature.workday.SingleProjectState
import com.akiwiksten.worktime30.feature.workday.WorkdayUiState

internal fun SingleProjectState.withDefaultAllowance(defaultAllowance: String): SingleProjectState {
    return if (allowance.isBlank()) copy(allowance = defaultAllowance) else this
}

internal fun resolveInitialSingleProjectState(
    initialSingleProjectState: SingleProjectState,
    projectsUiState: WorkdayUiState
): SingleProjectState {
    val hasNavigationPayload = initialSingleProjectState.projectName.isNotBlank() ||
        initialSingleProjectState.projectTime != ZERO_TIME ||
        initialSingleProjectState.projectDetails != null ||
        initialSingleProjectState.workStats != null

    return when {
        initialSingleProjectState.index == -1 || hasNavigationPayload -> initialSingleProjectState
        else -> (projectsUiState as? WorkdayUiState.Success)
            ?.projects
            ?.find { it.index == initialSingleProjectState.index }
            ?: initialSingleProjectState
    }
}

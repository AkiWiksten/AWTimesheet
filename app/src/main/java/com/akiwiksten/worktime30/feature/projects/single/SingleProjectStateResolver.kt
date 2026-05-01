package com.akiwiksten.worktime30.feature.projects.single

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.feature.workday.WorkdayUiState

internal fun SingleProjectState.withDefaultAllowance(defaultAllowance: String): SingleProjectState {
    return if (allowance.isBlank()) copy(allowance = defaultAllowance) else this
}

internal fun SingleProjectState.withDefaultWorkType(defaultWorkType: String): SingleProjectState {
    return if (workType.isBlank()) copy(workType = defaultWorkType) else this
}

internal fun resolveInitialSingleProjectState(
    initialSingleProjectState: SingleProjectState,
    initialProjectDetails: ProjectDetailsState?,
    initialSettings: SettingsState?,
    projectsUiState: WorkdayUiState
): SingleProjectState {
    val hasNavigationPayload = initialSingleProjectState.projectName.isNotBlank() ||
        initialSingleProjectState.projectTime != ZERO_TIME ||
        initialProjectDetails != null ||
        initialSettings != null

    return when {
        initialSingleProjectState.index == -1 || hasNavigationPayload -> initialSingleProjectState
        else -> (projectsUiState as? WorkdayUiState.Success)
            ?.projects
            ?.find { it.index == initialSingleProjectState.index }
            ?: initialSingleProjectState
    }
}

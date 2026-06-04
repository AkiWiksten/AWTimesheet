package com.akiwiksten.awtimesheet.feature.singleproject.model

import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.singleproject.SingleProjectUiState

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
    singleProjectUiState: SingleProjectUiState
): SingleProjectState {
    val hasNavigationPayload = initialSingleProjectState.projectName.isNotBlank() ||
        initialSingleProjectState.projectTime != ZERO_TIME ||
        initialProjectDetails != null ||
        initialSettings != null

    return when {
        initialSingleProjectState.index == -1 || hasNavigationPayload -> initialSingleProjectState
        else -> (singleProjectUiState as? SingleProjectUiState.Success)
            ?.data!!
    }
}


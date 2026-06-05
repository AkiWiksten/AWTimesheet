package com.akiwiksten.awtimesheet.feature.singleproject.model

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

internal fun SingleProjectState.withFlexDayLogic(
    previousState: SingleProjectState,
    noAllowanceText: String,
    flexDayWorkType: String
): SingleProjectState {
    val isFlexDay = workType.equals(flexDayWorkType, ignoreCase = true)
    val workTypeChanged = workType != previousState.workType
    return if (isFlexDay && workTypeChanged) {
        copy(kilometres = "0", allowance = noAllowanceText)
    } else {
        this
    }
}

internal fun SingleProjectState.withAbsenceLogic(
    previousState: SingleProjectState,
    settings: SettingsState?,
    absencePrefix: String
): SingleProjectState {
    val isAbsence = workType.startsWith(prefix = absencePrefix, ignoreCase = true)
    val workTypeChanged = workType != previousState.workType
    return if (isAbsence && workTypeChanged) {
        val estimate = settings?.dailyWorkTimeEstimate
        if (!estimate.isNullOrBlank()) {
            copy(projectTime = estimate, projectName = workType)
        } else {
            copy(projectName = workType)
        }
    } else {
        this
    }
}

internal fun SingleProjectState.withInitialAbsenceLogic(
    isAddMode: Boolean,
    settings: SettingsState?,
    absencePrefix: String
): SingleProjectState {
    val isAbsence = workType.startsWith(prefix = absencePrefix, ignoreCase = true)
    return if (isAddMode && isAbsence) {
        val updatedTime = if (projectTime == ZERO_TIME) {
            settings?.dailyWorkTimeEstimate?.takeIf { it.isNotBlank() } ?: projectTime
        } else {
            projectTime
        }
        val updatedName = projectName.ifBlank { workType }
        copy(projectTime = updatedTime, projectName = updatedName)
    } else {
        this
    }
}

internal fun resolveFullInitialSingleProjectState(
    args: SingleProjectScreenArgs,
    uiState: SingleProjectUiState,
    noAllowanceText: String,
    defaultWorkTypeText: String,
    absencePrefix: String
): SingleProjectState {
    val settings = (uiState as? SingleProjectUiState.Success)?.settings
        ?: args.initialSettings
    return resolveInitialSingleProjectState(
        initialSingleProjectState = args.initialSingleProjectState,
        initialProjectDetails = args.initialProjectDetails,
        initialSettings = settings,
        singleProjectUiState = uiState
    )
        .withDefaultAllowance(defaultAllowance = noAllowanceText)
        .withDefaultWorkType(defaultWorkType = defaultWorkTypeText)
        .withInitialAbsenceLogic(
            isAddMode = args.initialSingleProjectState.index == -1,
            settings = settings,
            absencePrefix = absencePrefix
        )
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

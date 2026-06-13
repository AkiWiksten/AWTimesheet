package com.akiwiksten.awtimesheet.feature.singleproject.model

import com.akiwiksten.awtimesheet.core.ZERO_TIME
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
        copy(kilometres = "", allowance = noAllowanceText)
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
        val updatedTime = if (!estimate.isNullOrBlank()) estimate else projectTime
        copy(
            projectTime = updatedTime,
            projectName = workType,
            kilometres = "",
            allowance = ""
        )
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
        copy(
            projectTime = updatedTime,
            projectName = updatedName,
            kilometres = "",
            allowance = ""
        )
    } else {
        this
    }
}

internal fun resolveFullInitialSingleProjectState(
    uiState: SingleProjectUiState,
    noAllowanceText: String,
    defaultWorkTypeText: String,
    absencePrefix: String
): SingleProjectState {
    val settings = (uiState as? SingleProjectUiState.Success)?.settings
    return resolveInitialSingleProjectState(
        initialSingleProjectState = (uiState as? SingleProjectUiState.Success)?.data,
        singleProjectUiState = uiState
    )
        .withDefaultAllowance(defaultAllowance = noAllowanceText)
        .withDefaultWorkType(defaultWorkType = defaultWorkTypeText)
        .withInitialAbsenceLogic(
            isAddMode = (uiState as SingleProjectUiState.Success).data.isAddMode,
            settings = settings,
            absencePrefix = absencePrefix
        )
}

internal fun resolveInitialSingleProjectState(
    initialSingleProjectState: SingleProjectState?,
    singleProjectUiState: SingleProjectUiState
): SingleProjectState {
    val hasNavigationPayload = initialSingleProjectState?.projectName?.isNotBlank() == true ||
        initialSingleProjectState?.projectTime != ZERO_TIME

    return when {
        initialSingleProjectState?.isAddMode == true || hasNavigationPayload ->
            initialSingleProjectState ?: SingleProjectState()
        else -> (singleProjectUiState as? SingleProjectUiState.Success)
            ?.data!!
    }
}

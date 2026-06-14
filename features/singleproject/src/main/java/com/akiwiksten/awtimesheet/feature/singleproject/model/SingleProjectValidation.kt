package com.akiwiksten.awtimesheet.feature.singleproject.model

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.isActionEnabled
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState

internal fun isDuplicateProjectName(
    projectName: String,
    otherProjectNames: List<String>,
): Boolean {
    if (projectName.isBlank()) return false
    return otherProjectNames.any { it.equals(projectName, ignoreCase = true) }
}

internal fun isSingleProjectConfirmEnabled(
    state: SingleProjectState,
    hasUnsavedChanges: Boolean,
    isDuplicateProjectName: Boolean,
    isAddMode: Boolean,
    hasProjectDetails: Boolean = false,
): Boolean {
    if (isDuplicateProjectName) return false
    val hasProjectName = state.projectName.isNotBlank()
    val hasProjectTime = state.projectTime.isNotBlank() &&
        ((state.projectTime != ZERO_TIME) || hasProjectDetails)

    val hasProjectNameAndTime = hasProjectName && hasProjectTime

    val hasRequiredFields = hasProjectNameAndTime &&
        (state.kilometres.isBlank() || state.kilometres.all(Char::isDigit))
    return isActionEnabled(
        hasRequiredFields = hasRequiredFields,
        hasUnsavedChanges = hasUnsavedChanges,
        allowWithoutChanges = isAddMode || hasProjectNameAndTime
    )
}

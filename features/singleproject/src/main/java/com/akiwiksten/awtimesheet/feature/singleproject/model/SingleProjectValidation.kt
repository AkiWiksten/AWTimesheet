package com.akiwiksten.awtimesheet.feature.singleproject.model

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
): Boolean {
    if (isDuplicateProjectName) return false
    val hasProjectName = state.projectName.isNotBlank()

    val hasRequiredFields = hasProjectName &&
        (state.kilometres.isBlank() || state.kilometres.all(Char::isDigit))

    return isActionEnabled(
        hasRequiredFields = hasRequiredFields,
        hasUnsavedChanges = hasUnsavedChanges,
        allowWithoutChanges = true
    )
}

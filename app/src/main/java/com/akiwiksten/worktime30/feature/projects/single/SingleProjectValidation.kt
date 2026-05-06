package com.akiwiksten.worktime30.feature.projects.single

import com.akiwiksten.worktime30.core.ui.isActionEnabled
import com.akiwiksten.worktime30.domain.model.SingleProjectState

internal fun isDuplicateProjectName(
    projectName: String,
    currentIndex: Int,
    singleProjectState: SingleProjectState?,
): Boolean {
    if (projectName.isBlank()) return false
    return singleProjectState?.index != currentIndex &&
            singleProjectState?.projectName.equals(projectName, ignoreCase = true)
}

internal fun isSingleProjectConfirmEnabled(
    state: SingleProjectState,
    hasUnsavedChanges: Boolean,
    isDuplicateProjectName: Boolean,
    isAddMode: Boolean
): Boolean {
    if (isDuplicateProjectName) return false
    val hasProjectNameAndTime = state.projectName.isNotBlank() && state.projectTime.isNotBlank()
    val hasRequiredFields = hasProjectNameAndTime &&
        (state.kilometres.isBlank() || state.kilometres.all(Char::isDigit))
    return isActionEnabled(
        hasRequiredFields = hasRequiredFields,
        hasUnsavedChanges = hasUnsavedChanges,
        allowWithoutChanges = isAddMode || hasProjectNameAndTime
    )
}

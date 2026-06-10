package com.akiwiksten.awtimesheet.feature.singleproject.model

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.isActionEnabled
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState

internal fun isDuplicateProjectName(
    projectName: String,
    currentIndex: Int,
    singleProjectState: SingleProjectState?,
): Boolean {
    if (projectName.isBlank() || currentIndex != -1) return false
    return singleProjectState?.listIndex != currentIndex &&
        singleProjectState?.projectName.equals(projectName, ignoreCase = true)
}

internal fun isSingleProjectConfirmEnabled(
    state: SingleProjectState,
    hasUnsavedChanges: Boolean,
    isDuplicateProjectName: Boolean,
    isAddMode: Boolean
): Boolean {
    if (isDuplicateProjectName) return false
    val hasProjectNameAndTime =
        state.projectName.isNotBlank() &&
            state.projectTime.isNotBlank() &&
            state.projectTime != ZERO_TIME
    val hasRequiredFields = hasProjectNameAndTime &&
        (state.kilometres.isBlank() || state.kilometres.all(Char::isDigit))
    return isActionEnabled(
        hasRequiredFields = hasRequiredFields,
        hasUnsavedChanges = hasUnsavedChanges,
        allowWithoutChanges = isAddMode || hasProjectNameAndTime
    )
}

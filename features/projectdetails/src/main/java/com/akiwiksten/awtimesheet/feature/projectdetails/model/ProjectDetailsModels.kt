package com.akiwiksten.awtimesheet.feature.projectdetails.model

import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState

enum class ProjectDetailsField {
    START_TIME,
    END_TIME,
    LUNCH_START,
    LUNCH_END,
    LUNCH_TIME,
    BREAK_START,
    BREAK_END,
    PROJECT_TIME
}

data class ProjectDetailsTimeFieldAction(
    val onCurrent: () -> Unit = {},
    val onSet: (String) -> Unit = {}
)

data class ProjectDetailsFieldActions(
    val startTime: ProjectDetailsTimeFieldAction = ProjectDetailsTimeFieldAction(),
    val lunchTime: ProjectDetailsTimeFieldAction = ProjectDetailsTimeFieldAction(),
    val endTime: ProjectDetailsTimeFieldAction = ProjectDetailsTimeFieldAction(),
    val projectTime: ProjectDetailsTimeFieldAction = ProjectDetailsTimeFieldAction(),
    val lunchStart: ProjectDetailsTimeFieldAction = ProjectDetailsTimeFieldAction(),
    val lunchEnd: ProjectDetailsTimeFieldAction = ProjectDetailsTimeFieldAction(),
    val breakStart: ProjectDetailsTimeFieldAction = ProjectDetailsTimeFieldAction(),
    val breakEnd: ProjectDetailsTimeFieldAction = ProjectDetailsTimeFieldAction(),
)

data class ProjectDetailsScreenActions(
    val onClearDetails: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val fieldActions: ProjectDetailsFieldActions = ProjectDetailsFieldActions()
)

data class ProjectDetailsTimeRowLabels(
    val currentTimeLabelId: Int? = null,
    val timePickerLabelId: Int? = null,
)

data class ProjectDetailsDisplayParams(
    val state: ProjectDetailsState,
    val actions: ProjectDetailsScreenActions,
    val isConfirmEnabled: Boolean,
    val isAddMode: Boolean = true
)

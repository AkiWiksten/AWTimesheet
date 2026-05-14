package com.akiwiksten.worktime30.feature.projects.details

data class TimeFieldAction(
    val onCurrent: () -> Unit = {},
    val onSet: (String) -> Unit = {}
)

data class ProjectDetailsFieldActions(
    val startTime: TimeFieldAction = TimeFieldAction(),
    val lunchTime: TimeFieldAction = TimeFieldAction(),
    val endTime: TimeFieldAction = TimeFieldAction(),
    val projectTime: TimeFieldAction = TimeFieldAction(),
    val lunchStart: TimeFieldAction = TimeFieldAction(),
    val lunchEnd: TimeFieldAction = TimeFieldAction(),
    val breakStart: TimeFieldAction = TimeFieldAction(),
    val breakEnd: TimeFieldAction = TimeFieldAction(),
)

data class ProjectDetailsScreenActions(
    val onClearDetails: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val fieldActions: ProjectDetailsFieldActions = ProjectDetailsFieldActions()
)

data class TimeRowLabels(
    val currentTimeLabelId: Int? = null,
    val timePickerLabelId: Int? = null,
)

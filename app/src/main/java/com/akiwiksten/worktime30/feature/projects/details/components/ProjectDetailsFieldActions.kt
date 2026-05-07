package com.akiwiksten.worktime30.feature.projects.details.components

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

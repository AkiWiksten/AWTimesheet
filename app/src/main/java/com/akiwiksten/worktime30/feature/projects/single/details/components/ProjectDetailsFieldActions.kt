package com.akiwiksten.worktime30.feature.projects.single.details.components

data class ProjectDetailsFieldActions(
    val onCurrentStartTime: () -> Unit = {},
    val onSetStartTime: (String) -> Unit = {},
    val onCurrentLunchTime: () -> Unit = {},
    val onSetLunchTime: (String) -> Unit = {},
    val onCurrentEndTime: () -> Unit = {},
    val onSetEndTime: (String) -> Unit = {},
    val onCurrentProjectTime: () -> Unit = {},
    val onSetProjectTime: (String) -> Unit = {},
    val onCurrentLunchStart: () -> Unit = {},
    val onSetLunchStart: (String) -> Unit = {},
    val onCurrentLunchEnd: () -> Unit = {},
    val onSetLunchEnd: (String) -> Unit = {},
    val onCurrentBreakStart: () -> Unit = {},
    val onSetBreakStart: (String) -> Unit = {},
    val onCurrentBreakEnd: () -> Unit = {},
    val onSetBreakEnd: (String) -> Unit = {}
)

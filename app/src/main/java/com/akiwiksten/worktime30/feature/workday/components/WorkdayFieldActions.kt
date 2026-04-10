package com.akiwiksten.worktime30.feature.workday.components

data class WorkdayFieldActions(
    val onCurrentStartTime: () -> Unit = {},
    val onSetStartTime: (String) -> Unit = {},
    val onCurrentDailyWorkTime: () -> Unit = {},
    val onSetDailyWorkTime: (String) -> Unit = {},
    val onCurrentLunchTime: () -> Unit = {},
    val onSetLunchTime: (String) -> Unit = {},
    val onSetBalanceTotal: (String, Boolean) -> Unit = { _, _ -> },
    val onSetWorkTimeTotal: (String, Boolean) -> Unit = { _, _ -> },
    val onCurrentEndTime: () -> Unit = {},
    val onSetEndTime: (String) -> Unit = {},
    val onCurrentWorkTimeToday: () -> Unit = {},
    val onSetWorkTimeToday: (String) -> Unit = {},
    val onCurrentLunchStart: () -> Unit = {},
    val onSetLunchStart: (String) -> Unit = {},
    val onCurrentLunchEnd: () -> Unit = {},
    val onSetLunchEnd: (String) -> Unit = {},
    val onCurrentBreakStart: () -> Unit = {},
    val onSetBreakStart: (String) -> Unit = {},
    val onCurrentBreakEnd: () -> Unit = {},
    val onSetBreakEnd: (String) -> Unit = {},
    val onSetBalanceToday: (String, Boolean) -> Unit = { _, _ -> }
)

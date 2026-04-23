package com.akiwiksten.worktime30.feature.workday

data class WorkdayActions(
    val onSelectedItemIndexChange: (Int) -> Unit,
    val onNavigateToSingleProject: (Int) -> Unit,
    val onRetry: () -> Unit,
    val onSaveWorkStats: (String, String) -> Unit,
    val onDeleteProject: (SingleProjectState) -> Unit
)

internal data class WorkStatsEditorState(
    val dailyWorkTime: String,
    val balanceTotal: String,
    val isDailyWorkTimeError: Boolean,
    val isBalanceTotalError: Boolean,
    val hasUnsavedChanges: Boolean
)

internal data class WorkdayHeaderActions(
    val onDailyWorkTimeChange: (String) -> Unit,
    val onBalanceTotalChange: (String) -> Unit,
    val onSaveWorkStats: () -> Unit
)

internal val DAILY_WORK_TIME_INPUT_REGEX = Regex(pattern = "(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")
internal val FLEX_TIME_TOTAL_INPUT_REGEX = Regex(pattern = "[+-]?(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")



package com.akiwiksten.worktime30.feature.workday

import com.akiwiksten.worktime30.domain.model.SingleProjectState

data class WorkdayActions(
    val onSelectedItemIndexChange: (Int) -> Unit,
    val onNavigateToSingleProject: (Int) -> Unit,
    val onRetry: () -> Unit,
    val onSaveSettings: (String) -> Unit,
    val onDeleteProject: (SingleProjectState) -> Unit
)

internal data class SettingsEditorState(
    val workTimeTodayEstimate: String,
    val isWorkTimeTodayEstimateError: Boolean
)

internal data class WorkdayHeaderActions(
    val onWorkTimeTodayEstimateChange: (String) -> Unit
)

internal val WORK_TIME_TODAY_ESTIMATE_INPUT_REGEX = Regex(pattern = "(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")

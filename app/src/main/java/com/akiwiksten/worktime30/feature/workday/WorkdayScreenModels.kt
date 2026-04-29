package com.akiwiksten.worktime30.feature.workday

import com.akiwiksten.worktime30.domain.model.SingleProjectState

data class WorkdayActions(
    val onSelectedItemIndexChange: (Int) -> Unit,
    val onNavigateToSingleProject: (Int) -> Unit,
    val onRetry: () -> Unit,
    val onSaveSettings: (String, String) -> Unit,
    val onDeleteProject: (SingleProjectState) -> Unit
)

internal data class SettingsEditorState(
    val workTimeTodayEstimate: String,
    val initialFlexTimeTotal: String,
    val isWorkTimeTodayEstimateError: Boolean,
    val isInitialFlexTimeTotalError: Boolean,
    val hasUnsavedChanges: Boolean
)

internal data class WorkdayHeaderActions(
    val onWorkTimeTodayEstimateChange: (String) -> Unit,
    val onInitialFlexTimeTotalChange: (String) -> Unit,
    val onSaveSettings: () -> Unit
)

internal val WORK_TIME_TODAY_ESTIMATE_INPUT_REGEX = Regex(pattern = "(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")
internal val INITIAL_FLEX_TIME_TOTAL_INPUT_REGEX = Regex(pattern = "[+-]?(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")

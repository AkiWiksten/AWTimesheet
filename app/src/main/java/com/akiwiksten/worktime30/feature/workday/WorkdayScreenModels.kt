package com.akiwiksten.worktime30.feature.workday

import com.akiwiksten.worktime30.domain.model.SingleProjectState

data class WorkdayActions(
    val onSelectedItemIndexChange: (Int) -> Unit,
    val onNavigateToSingleProject: (SingleProjectState) -> Unit,
    val onRetry: () -> Unit,
    val onSaveSettings: (String, Boolean) -> Unit,
    val onDeleteProject: (SingleProjectState) -> Unit
)

internal data class SettingsEditorState(
    val workTimeByDateEstimate: String,
    val isWorkTimeByDateEstimateError: Boolean
)

internal data class WorkdayHeaderActions(
    val onWorkTimeByDateEstimateChange: (String) -> Unit
)

internal val WORK_TIME_BY_DATE_ESTIMATE_INPUT_REGEX = Regex(pattern = "(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")

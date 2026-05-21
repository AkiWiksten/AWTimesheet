package com.akiwiksten.awtimesheet.feature.workday

import com.akiwiksten.awtimesheet.domain.model.SingleProjectState

data class WorkdayActions(
    val onSelectedItemIndexChange: (Int) -> Unit,
    val onTrackProjectEditorLaunch: (oldFlexTimeByDate: String, oldWorkTimeByDate: String) -> Unit,
    val onNavigateToSingleProject: (SingleProjectState) -> Unit,
    val onRetry: () -> Unit,
    val onSaveSettings: (String, Boolean) -> Unit,
    val onDeleteProject: (SingleProjectState) -> Unit
)

internal data class WorkdaySettingsEditorState(
    val workTimeByDateEstimate: String,
    val isWorkTimeByDateEstimateError: Boolean
)

internal data class WorkdayHeaderActions(
    val onWorkTimeByDateEstimateChange: (String) -> Unit
)

internal val WORK_TIME_BY_DATE_ESTIMATE_INPUT_REGEX = Regex(pattern = "(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")


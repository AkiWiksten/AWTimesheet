package com.akiwiksten.awtimesheet.feature.workday.model

import androidx.compose.runtime.Immutable
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState

sealed class WorkdayUiState {
    object Loading : WorkdayUiState()

    data class Success(
        val date: String = "",
        val workTimeByDate: String = ZERO_TIME,
        val workTimeByDateEstimate: String = ZERO_TIME,
        val flexTimeByDate: String = ZERO_TIME,
        val initialFlexTimeTotal: String = ZERO_TIME,
        val flexTimeTotal: String = ZERO_TIME,
        val projects: List<SingleProjectState> = emptyList(),
        val workTypes: List<String> = emptyList()
    ) : WorkdayUiState()

    data class Error(val message: String) : WorkdayUiState()
}

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

@Immutable
internal data class WorkdayListItemUiModel(
    val index: Int,
    val projectName: String,
    val projectTime: String,
    val kilometres: String,
    val allowance: String,
    val workType: String,
    val kilometresLabel: String,
    val isProjectNameOnlyPlaceholder: Boolean,
    val stableKey: String
)

internal data class WorkdayStatsCardState(
    val workTime: String,
    val flexTimeByDate: String,
    val calculatedFlexTimeTotal: String,
    val editorState: WorkdaySettingsEditorState
)

internal data class WorkdayEstimateUiState(
    val workTimeByDateEstimate: String,
    val isWorkTimeByDateEstimateValid: Boolean,
    val onWorkTimeByDateEstimateChange: (String) -> Unit
)

internal data class WorkdayDisplayState(
    val displayedFlexTimeByDate: String,
    val displayedCalculatedFlexTimeTotal: String,
    val editorState: WorkdaySettingsEditorState,
    val headerActions: WorkdayHeaderActions
)

internal data class WorkdayStatsCardContentParams(
    val workTime: String,
    val flexTimeByDate: String,
    val calculatedFlexTimeTotal: String,
    val editorState: WorkdaySettingsEditorState,
    val onWorkTimeByDateEstimatePickerClick: () -> Unit
)

internal val WORK_TIME_BY_DATE_ESTIMATE_INPUT_REGEX = Regex(pattern = "(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")

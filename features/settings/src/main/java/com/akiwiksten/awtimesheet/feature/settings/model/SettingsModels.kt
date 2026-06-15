package com.akiwiksten.awtimesheet.feature.settings.model

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.MutableState
import com.akiwiksten.awtimesheet.domain.usecase.GeneratedAllowanceLabels
import com.akiwiksten.awtimesheet.feature.settings.SettingsUiState
import com.akiwiksten.awtimesheet.feature.settings.SettingsViewModel

data class SettingsActions(
    val onNameChange: (String) -> Unit,
    val onEmployerChange: (String) -> Unit,
    val onDailyWorkTimeEstimateChange: (String) -> Unit,
    val onDailyLunchTimeEstimateChange: (String) -> Unit,
    val onInitialFlexTimeTotalChange: (String) -> Unit,
    val onWorkTypeAdded: (String) -> Unit,
    val onWorkTypeRemoved: (String) -> Unit,
    val onEnableTestFeaturesChange: (Boolean) -> Unit,
    val onSave: () -> Unit,
    val onGenerateXlsx: () -> Unit,
    val onGenerateWorkdaysForMonth: () -> Unit,
    val onGenerateWorkdaysForYear: () -> Unit
)

internal data class SettingsWorkTypeDialogState(
    val selectedWorkType: String,
    val onWorkTypeSelected: (String) -> Unit,
    val onAddClick: () -> Unit,
    val onDeleteClick: () -> Unit
)

internal data class SettingsWorkTypeSectionState(
    val workTypes: List<String>,
    val settingsWorkTypeDialogState: SettingsWorkTypeDialogState,
    val protectedWorkTypes: List<String>
)

internal data class SettingsAddWorkTypeDialogState(
    val isVisible: Boolean,
    val onDismiss: () -> Unit,
    val onConfirm: (String) -> Unit
)

internal data class SettingsTimePickerState(
    val onDailyWorkTimePickerClick: () -> Unit,
    val onDailyLunchTimeEstimatePickerClick: () -> Unit
)

internal data class SettingsContentBodyState(
    val uiState: SettingsUiState.Success,
    val actions: SettingsActions,
    val settingsWorkTypeState: SettingsWorkTypeDialogState,
    val settingsTimePickerState: SettingsTimePickerState,
    val settingsAddWorkTypeDialogState: SettingsAddWorkTypeDialogState,
    val scrollState: ScrollState,
    val settingsSaveUi: SettingsSaveUi,
    val defaultWorkTypes: List<String>
)

internal data class SettingsWorkTypeUiState(
    val settingsWorkTypeDialogState: SettingsWorkTypeDialogState,
    val settingsAddWorkTypeDialogState: SettingsAddWorkTypeDialogState
)

internal data class SettingsTimePickerDialogConfig(
    val time: String,
    val isVisible: Boolean,
    val onDismiss: () -> Unit,
    val onConfirm: (String) -> Unit
)

internal data class SettingsSaveUi(
    val isSaveEnabled: Boolean,
    val hasUnsavedChanges: Boolean,
    val isInitialFlexTimeTotalError: Boolean,
    val onSaveRequested: () -> Unit
)

internal data class SettingsScreenBodyState(
    val uiState: SettingsUiState,
    val defaultWorkTypes: List<String>,
    val generatedAllowanceLabels: GeneratedAllowanceLabels,
    val onUnsavedChangesChanged: (Boolean) -> Unit,
    val registerUnsavedActions: (onSave: (() -> Unit)?, onDiscard: (() -> Unit)?) -> Unit,
    val onDiscardChanges: () -> Unit,
    val settingsViewModel: SettingsViewModel
)

internal data class SettingsStateContentState(
    val uiState: SettingsUiState,
    val defaultWorkTypes: List<String>,
    val onUnsavedChangesChanged: (Boolean) -> Unit,
    val registerUnsavedActions: (onSave: (() -> Unit)?, onDiscard: (() -> Unit)?) -> Unit,
    val onDiscardChanges: () -> Unit,
    val createActions: (SettingsUiState.Success) -> SettingsActions
)

internal data class SettingsLoadingContentState(
    val showLoadingIndicator: Boolean,
    val lastSuccessState: SettingsUiState.Success?,
    val defaultWorkTypes: List<String>,
    val onUnsavedChangesChanged: (Boolean) -> Unit,
    val registerUnsavedActions: (onSave: (() -> Unit)?, onDiscard: (() -> Unit)?) -> Unit,
    val onDiscardChanges: () -> Unit,
    val createActions: (SettingsUiState.Success) -> SettingsActions
)

internal data class SettingsActionButtonsSectionState(
    val onSave: () -> Unit,
    val onGenerateXlsx: () -> Unit,
    val onGenerateWorkdaysForMonth: () -> Unit,
    val onGenerateWorkdaysForYear: () -> Unit,
    val isReportEnabled: Boolean,
    val isSaveEnabled: Boolean,
    val isTestFeaturesEnabled: Boolean
)

internal data class SettingsContentState(
    val uiState: SettingsUiState.Success,
    val actions: SettingsActions,
    val defaultWorkTypes: List<String>,
    val onUnsavedChangesChanged: (Boolean) -> Unit = {},
    val registerUnsavedActions: (onSave: (() -> Unit)?, onDiscard: (() -> Unit)?) -> Unit = { _, _ -> },
    val onDiscardChanges: () -> Unit = {}
)

internal data class SettingsDialogVisibilityState(
    val showWorkTimePicker: MutableState<Boolean>,
    val showLunchTimePicker: MutableState<Boolean>,
    val showGenerateMonthlyReportConfirm: MutableState<Boolean>,
    val showGenerateMonthConfirm: MutableState<Boolean>,
    val showGenerateYearConfirm: MutableState<Boolean>
)

package com.akiwiksten.awtimesheet.feature.settings

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.core.hasChanges
import com.akiwiksten.awtimesheet.domain.model.SettingsState

// State data classes
data class SettingsActions(
    val onNameChange: (String) -> Unit,
    val onEmployerChange: (String) -> Unit,
    val onDailyWorkTimeEstimateChange: (String) -> Unit,
    val onDailyLunchTimeEstimateChange: (String) -> Unit,
    val onInitialFlexTimeTotalChange: (String) -> Unit,
    val onWorkTypeAdded: (String) -> Unit,
    val onWorkTypeRemoved: (String) -> Unit,
    val onSave: () -> Unit,
    val onGenerateXlsx: () -> Unit
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
    val protectedWorkType: String
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
    val scrollState: androidx.compose.foundation.ScrollState,
    val settingsSaveUi: SettingsSaveUi,
    val defaultWorkType: String
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
    val isInitialFlexTimeTotalError: Boolean,
    val onSaveRequested: () -> Unit
)

// Utility functions
@Composable
internal fun rememberSettingsWorkTypeUiState(
    workTypes: List<String>,
    defaultWorkType: String,
    onWorkTypeRemoved: (String) -> Unit,
    onWorkTypeAdded: (String) -> Unit
): SettingsWorkTypeUiState {
    val context = LocalContext.current
    val protectedMessage = stringResource(id = R.string.default_work_type_cannot_be_deleted)
    val showAddDialog = remember { mutableStateOf(value = false) }
    val selectedWorkType = remember(defaultWorkType) { mutableStateOf(value = defaultWorkType) }

    LaunchedEffect(workTypes, defaultWorkType, selectedWorkType.value) {
        if (selectedWorkType.value.isBlank() || selectedWorkType.value !in workTypes) {
            selectedWorkType.value = defaultWorkType
        }
    }

    return SettingsWorkTypeUiState(
        settingsWorkTypeDialogState = SettingsWorkTypeDialogState(
            selectedWorkType = selectedWorkType.value,
            onWorkTypeSelected = { selectedWorkType.value = it },
            onAddClick = { showAddDialog.value = true },
            onDeleteClick = {
                if (selectedWorkType.value != defaultWorkType) {
                    onWorkTypeRemoved(selectedWorkType.value)
                    selectedWorkType.value = defaultWorkType
                } else {
                    Toast.makeText(context, protectedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        ),
        settingsAddWorkTypeDialogState = SettingsAddWorkTypeDialogState(
            isVisible = showAddDialog.value,
            onDismiss = { showAddDialog.value = false },
            onConfirm = {
                onWorkTypeAdded(it)
                selectedWorkType.value = it
                showAddDialog.value = false
            }
        )
    )
}

@Composable
internal fun rememberSettingsSaveUi(
    data: SettingsState,
    selectedDate: String,
    onSave: () -> Unit
): SettingsSaveUi {
    val context = LocalContext.current
    val savedText = stringResource(id = R.string.saved)
    val lastSavedNameState = remember(selectedDate) { mutableStateOf(value = data.name) }
    val lastSavedEmployerState = remember(selectedDate) { mutableStateOf(value = data.employer) }
    val lastSavedDailyWorkTimeEstimateState = remember(selectedDate) {
        mutableStateOf(value = data.dailyWorkTimeEstimate)
    }
    val lastSavedDailyLunchTimeEstimateState = remember(selectedDate) {
        mutableStateOf(value = data.dailyLunchTimeEstimate)
    }
    val lastSavedInitialFlexTimeTotalState = remember(selectedDate) {
        mutableStateOf(value = data.initialFlexTimeTotal)
    }
    val lastSavedWorkTypesState = remember(selectedDate) { mutableStateOf(value = data.workTypes) }

    val hasUnsavedChanges =
        hasChanges(current = data.name, baseline = lastSavedNameState.value) ||
            hasChanges(current = data.employer, baseline = lastSavedEmployerState.value) ||
            hasChanges(
                current = data.dailyWorkTimeEstimate,
                baseline = lastSavedDailyWorkTimeEstimateState.value
            ) ||
            hasChanges(
                current = data.dailyLunchTimeEstimate,
                baseline = lastSavedDailyLunchTimeEstimateState.value
            ) ||
            hasChanges(
                current = data.initialFlexTimeTotal,
                baseline = lastSavedInitialFlexTimeTotalState.value
            ) ||
            hasChanges(current = data.workTypes, baseline = lastSavedWorkTypesState.value)
    val isInitialFlexTimeTotalError = remember(data.initialFlexTimeTotal) {
        !data.initialFlexTimeTotal.matches(INITIAL_FLEX_TIME_TOTAL_INPUT_REGEX)
    }

    return SettingsSaveUi(
        isSaveEnabled = com.akiwiksten.awtimesheet.core.isActionEnabled(
            hasRequiredFields = !isInitialFlexTimeTotalError,
            hasUnsavedChanges = hasUnsavedChanges
        ),
        isInitialFlexTimeTotalError = isInitialFlexTimeTotalError,
        onSaveRequested = {
            if (hasUnsavedChanges) {
                onSave()
                lastSavedNameState.value = data.name
                lastSavedEmployerState.value = data.employer
                lastSavedDailyWorkTimeEstimateState.value = data.dailyWorkTimeEstimate
                lastSavedDailyLunchTimeEstimateState.value = data.dailyLunchTimeEstimate
                lastSavedInitialFlexTimeTotalState.value = data.initialFlexTimeTotal
                lastSavedWorkTypesState.value = data.workTypes
                Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
            }
        }
    )
}

private val INITIAL_FLEX_TIME_TOTAL_INPUT_REGEX = Regex(pattern = "[+-]?(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")


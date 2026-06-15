package com.akiwiksten.awtimesheet.feature.settings.remember

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.akiwiksten.awtimesheet.core.hasChanges
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.usecase.GeneratedAllowanceLabels
import com.akiwiksten.awtimesheet.feature.settings.INITIAL_FLEX_TIME_TOTAL_INPUT_REGEX
import com.akiwiksten.awtimesheet.feature.settings.R
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsAddWorkTypeDialogState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsDialogVisibilityState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsSaveUi
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsWorkTypeDialogState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsWorkTypeUiState
import com.akiwiksten.awtimesheet.core.R as CoreR

@Composable
internal fun rememberSettingsWorkTypeUiState(
    workTypes: List<String>,
    protectedWorkTypes: List<String>,
    onWorkTypeRemoved: (String) -> Unit,
    onWorkTypeAdded: (String) -> Unit
): SettingsWorkTypeUiState {
    val context = LocalContext.current
    val protectedMessage = stringResource(id = R.string.default_work_type_cannot_be_deleted)
    val showAddDialog = remember { mutableStateOf(value = false) }
    val defaultSelection = protectedWorkTypes.firstOrNull() ?: ""
    val selectedWorkType = remember(defaultSelection) { mutableStateOf(value = defaultSelection) }

    LaunchedEffect(workTypes, protectedWorkTypes, selectedWorkType.value) {
        if (selectedWorkType.value.isBlank() || selectedWorkType.value !in workTypes) {
            selectedWorkType.value = defaultSelection
        }
    }

    return SettingsWorkTypeUiState(
        settingsWorkTypeDialogState = SettingsWorkTypeDialogState(
            selectedWorkType = selectedWorkType.value,
            onWorkTypeSelected = { selectedWorkType.value = it },
            onAddClick = { showAddDialog.value = true },
            onDeleteClick = {
                if (selectedWorkType.value !in protectedWorkTypes) {
                    onWorkTypeRemoved(selectedWorkType.value)
                    selectedWorkType.value = defaultSelection
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
    val savedText = stringResource(id = CoreR.string.saved)
    val lastSavedState = remember(selectedDate) {
        SettingsSaveBaselines(
            name = mutableStateOf(data.name),
            employer = mutableStateOf(data.employer),
            dailyWorkTimeEstimate = mutableStateOf(data.dailyWorkTimeEstimate),
            dailyLunchTimeEstimate = mutableStateOf(data.dailyLunchTimeEstimate),
            initialFlexTimeTotal = mutableStateOf(data.initialFlexTimeTotal),
            workTypes = mutableStateOf(data.workTypes),
            enableTestFeatures = mutableStateOf(data.enableTestFeatures)
        )
    }

    val hasUnsavedChanges = hasSettingsUnsavedChanges(data = data, baseline = lastSavedState)
    val isInitialFlexTimeTotalError = remember(data.initialFlexTimeTotal) {
        !data.initialFlexTimeTotal.matches(INITIAL_FLEX_TIME_TOTAL_INPUT_REGEX)
    }

    return SettingsSaveUi(
        isSaveEnabled = com.akiwiksten.awtimesheet.core.isActionEnabled(
            hasRequiredFields = !isInitialFlexTimeTotalError,
            hasUnsavedChanges = hasUnsavedChanges
        ),
        hasUnsavedChanges = hasUnsavedChanges,
        isInitialFlexTimeTotalError = isInitialFlexTimeTotalError,
        onSaveRequested = {
            if (hasUnsavedChanges) {
                onSave()
                updateSettingsSaveBaselines(baseline = lastSavedState, data = data)
                Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
            }
        }
    )
}

private data class SettingsSaveBaselines(
    val name: androidx.compose.runtime.MutableState<String>,
    val employer: androidx.compose.runtime.MutableState<String>,
    val dailyWorkTimeEstimate: androidx.compose.runtime.MutableState<String>,
    val dailyLunchTimeEstimate: androidx.compose.runtime.MutableState<String>,
    val initialFlexTimeTotal: androidx.compose.runtime.MutableState<String>,
    val workTypes: androidx.compose.runtime.MutableState<List<String>>,
    val enableTestFeatures: androidx.compose.runtime.MutableState<Boolean>
)

private fun hasSettingsUnsavedChanges(
    data: SettingsState,
    baseline: SettingsSaveBaselines
): Boolean {
    return hasChanges(current = data.name, baseline = baseline.name.value) ||
        hasChanges(current = data.employer, baseline = baseline.employer.value) ||
        hasChanges(current = data.dailyWorkTimeEstimate, baseline = baseline.dailyWorkTimeEstimate.value) ||
        hasChanges(current = data.dailyLunchTimeEstimate, baseline = baseline.dailyLunchTimeEstimate.value) ||
        hasChanges(current = data.initialFlexTimeTotal, baseline = baseline.initialFlexTimeTotal.value) ||
        hasChanges(current = data.workTypes, baseline = baseline.workTypes.value) ||
        hasChanges(current = data.enableTestFeatures, baseline = baseline.enableTestFeatures.value)
}

private fun updateSettingsSaveBaselines(
    baseline: SettingsSaveBaselines,
    data: SettingsState
) {
    baseline.name.value = data.name
    baseline.employer.value = data.employer
    baseline.dailyWorkTimeEstimate.value = data.dailyWorkTimeEstimate
    baseline.dailyLunchTimeEstimate.value = data.dailyLunchTimeEstimate
    baseline.initialFlexTimeTotal.value = data.initialFlexTimeTotal
    baseline.workTypes.value = data.workTypes
    baseline.enableTestFeatures.value = data.enableTestFeatures
}

@Composable
internal fun rememberSettingsDialogVisibilityState() = SettingsDialogVisibilityState(
    showWorkTimePicker = remember { mutableStateOf(value = false) },
    showLunchTimePicker = remember { mutableStateOf(value = false) },
    showGenerateMonthlyReportConfirm = remember { mutableStateOf(value = false) },
    showGenerateMonthConfirm = remember { mutableStateOf(value = false) },
    showGenerateYearConfirm = remember { mutableStateOf(value = false) }
)

@Composable
internal fun rememberGeneratedAllowanceLabels(): GeneratedAllowanceLabels {
    val noAllowance = stringResource(id = CoreR.string.no_allowance)
    val fullAllowance = stringResource(id = CoreR.string.full_allowance)
    val halfDayAllowance = stringResource(id = CoreR.string.half_day_allowance)
    return remember(noAllowance, fullAllowance, halfDayAllowance) {
        GeneratedAllowanceLabels(
            noAllowance = noAllowance,
            fullAllowance = fullAllowance,
            halfDayAllowance = halfDayAllowance
        )
    }
}

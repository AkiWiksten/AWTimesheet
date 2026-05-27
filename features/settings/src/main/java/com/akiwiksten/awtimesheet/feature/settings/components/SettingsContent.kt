package com.akiwiksten.awtimesheet.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.akiwiksten.awtimesheet.core.FORM_SECTION_SPACING
import com.akiwiksten.awtimesheet.core.SCREEN_CONTENT_SPACING
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumn
import com.akiwiksten.awtimesheet.feature.settings.SettingsActions
import com.akiwiksten.awtimesheet.feature.settings.SettingsContentBodyState
import com.akiwiksten.awtimesheet.feature.settings.SettingsTimePickerDialogConfig
import com.akiwiksten.awtimesheet.feature.settings.SettingsTimePickerState
import com.akiwiksten.awtimesheet.feature.settings.SettingsUiState
import com.akiwiksten.awtimesheet.feature.settings.SettingsWorkTypeSectionState
import com.akiwiksten.awtimesheet.feature.settings.rememberSettingsSaveUi
import com.akiwiksten.awtimesheet.feature.settings.rememberSettingsWorkTypeUiState

@Composable
internal fun SettingsContent(
    uiState: SettingsUiState.Success,
    actions: SettingsActions,
    defaultWorkType: String,
    onUnsavedChangesChanged: (Boolean) -> Unit,
    registerUnsavedActions: (onSave: (() -> Unit)?, onDiscard: (() -> Unit)?) -> Unit,
    onDiscardChanges: () -> Unit
) {
    val showWorkTimePicker = remember { mutableStateOf(value = false) }
    val showLunchTimePicker = remember { mutableStateOf(value = false) }
    val showGenerateMonthConfirm = remember { mutableStateOf(value = false) }
    val showGenerateYearConfirm = remember { mutableStateOf(value = false) }
    val workTypeUi = rememberSettingsWorkTypeUiState(
        workTypes = uiState.data.workTypes,
        defaultWorkType = defaultWorkType,
        onWorkTypeRemoved = actions.onWorkTypeRemoved,
        onWorkTypeAdded = actions.onWorkTypeAdded
    )
    val saveUi = rememberSettingsSaveUi(
        data = uiState.data,
        selectedDate = uiState.selectedDate,
        onSave = actions.onSave
    )
    val latestOnDiscardChanges = rememberUpdatedState(onDiscardChanges)
    val latestOnSaveRequested = rememberUpdatedState(saveUi.onSaveRequested)

    LaunchedEffect(saveUi.hasUnsavedChanges) {
        onUnsavedChangesChanged(saveUi.hasUnsavedChanges)
    }

    LaunchedEffect(saveUi.hasUnsavedChanges, registerUnsavedActions) {
        registerUnsavedActions(latestOnSaveRequested.value, latestOnDiscardChanges.value)
    }
    val guardedActions = actions.copy(
        onGenerateWorkdaysForMonth = { showGenerateMonthConfirm.value = true },
        onGenerateWorkdaysForYear = { showGenerateYearConfirm.value = true }
    )

    SettingsTimePickerDialogsSection(
        workTimePicker = SettingsTimePickerDialogConfig(
            time = uiState.data.dailyWorkTimeEstimate,
            isVisible = showWorkTimePicker.value,
            onDismiss = { showWorkTimePicker.value = false },
            onConfirm = {
                actions.onDailyWorkTimeEstimateChange(it)
                showWorkTimePicker.value = false
            }
        ),
        lunchTimePicker = SettingsTimePickerDialogConfig(
            time = uiState.data.dailyLunchTimeEstimate,
            isVisible = showLunchTimePicker.value,
            onDismiss = { showLunchTimePicker.value = false },
            onConfirm = {
                actions.onDailyLunchTimeEstimateChange(it)
                showLunchTimePicker.value = false
            }
        )
    )

    SettingsContentBody(
        state = SettingsContentBodyState(
            uiState = uiState,
            actions = guardedActions,
            settingsWorkTypeState = workTypeUi.settingsWorkTypeDialogState,
            settingsTimePickerState = SettingsTimePickerState(
                onDailyWorkTimePickerClick = { showWorkTimePicker.value = true },
                onDailyLunchTimeEstimatePickerClick = { showLunchTimePicker.value = true }
            ),
            settingsAddWorkTypeDialogState = workTypeUi.settingsAddWorkTypeDialogState,
            scrollState = rememberScrollState(),
            settingsSaveUi = saveUi,
            defaultWorkType = defaultWorkType
        )
    )

    SettingsGenerateMonthConfirmDialogSection(
        isVisible = showGenerateMonthConfirm.value,
        onDismiss = { showGenerateMonthConfirm.value = false },
        onConfirmed = {
            showGenerateMonthConfirm.value = false
            actions.onGenerateWorkdaysForMonth()
        }
    )

    SettingsGenerateYearConfirmDialogSection(
        isVisible = showGenerateYearConfirm.value,
        onDismiss = { showGenerateYearConfirm.value = false },
        onConfirmed = {
            showGenerateYearConfirm.value = false
            actions.onGenerateWorkdaysForYear()
        }
    )
}

@Composable
private fun SettingsContentBody(
    state: SettingsContentBodyState
) {
    ScrollableScreenColumn(
        scrollState = state.scrollState,
        modifier = Modifier.fillMaxSize(),
        columnModifier = Modifier
            .fillMaxWidth()
            .padding(all = FORM_SECTION_SPACING),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = SCREEN_CONTENT_SPACING)
    ) {
        SettingsHeaderSection(date = state.uiState.selectedDate)

        SettingsCard {
            SettingsProfileSection(
                name = state.uiState.data.name,
                employer = state.uiState.data.employer,
                onNameChange = state.actions.onNameChange,
                onEmployerChange = state.actions.onEmployerChange
            )
        }

        SettingsGlobalDefaultsCard(state = state)

        SettingsCard {
            SettingsWorkTypeSection(
                state = SettingsWorkTypeSectionState(
                    workTypes = state.uiState.data.workTypes,
                    settingsWorkTypeDialogState = state.settingsWorkTypeState,
                    protectedWorkType = state.defaultWorkType
                )
            )
        }

        SettingsActionButtonsSection(
            onSave = state.settingsSaveUi.onSaveRequested,
            onGenerateXlsx = state.actions.onGenerateXlsx,
            onGenerateWorkdaysForMonth = state.actions.onGenerateWorkdaysForMonth,
            onGenerateWorkdaysForYear = state.actions.onGenerateWorkdaysForYear,
            isReportEnabled = state.uiState.selectedDate.isNotBlank(),
            isSaveEnabled = state.settingsSaveUi.isSaveEnabled
        )

        SettingsAddWorkTypeDialogSection(
            isVisible = state.settingsAddWorkTypeDialogState.isVisible,
            onDismiss = state.settingsAddWorkTypeDialogState.onDismiss,
            onConfirmed = state.settingsAddWorkTypeDialogState.onConfirm
        )
    }
}

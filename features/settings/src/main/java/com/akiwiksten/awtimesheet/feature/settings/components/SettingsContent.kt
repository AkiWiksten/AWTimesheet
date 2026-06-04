package com.akiwiksten.awtimesheet.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.akiwiksten.awtimesheet.core.FORM_SECTION_SPACING
import com.akiwiksten.awtimesheet.core.SCREEN_CONTENT_SPACING
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumn
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumnState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsActionButtonsSectionState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsActions
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsContentBodyState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsContentState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsDialogVisibilityState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsTimePickerDialogConfig
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsTimePickerState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsWorkTypeSectionState
import com.akiwiksten.awtimesheet.feature.settings.remember.rememberSettingsDialogVisibilityState
import com.akiwiksten.awtimesheet.feature.settings.remember.rememberSettingsSaveUi
import com.akiwiksten.awtimesheet.feature.settings.remember.rememberSettingsWorkTypeUiState

@Composable
internal fun SettingsContent(
    state: SettingsContentState
) {
    val dialogVisibility = rememberSettingsDialogVisibilityState()
    val workTypeUi = rememberSettingsWorkTypeUiState(
        workTypes = state.uiState.data.workTypes,
        defaultWorkType = state.defaultWorkType,
        onWorkTypeRemoved = state.actions.onWorkTypeRemoved,
        onWorkTypeAdded = state.actions.onWorkTypeAdded
    )
    val saveUi = rememberSettingsSaveUi(
        data = state.uiState.data,
        selectedDate = state.uiState.selectedDate,
        onSave = state.actions.onSave
    )

    SettingsUnsavedChangesEffects(
        saveUiHasUnsavedChanges = saveUi.hasUnsavedChanges,
        onUnsavedChangesChanged = state.onUnsavedChangesChanged,
        registerUnsavedActions = state.registerUnsavedActions,
        onSaveRequested = saveUi.onSaveRequested,
        onDiscardChanges = state.onDiscardChanges
    )

    val guardedActions = state.actions.copy(
        onGenerateXlsx = { dialogVisibility.showGenerateMonthlyReportConfirm.value = true },
        onGenerateWorkdaysForMonth = { dialogVisibility.showGenerateMonthConfirm.value = true },
        onGenerateWorkdaysForYear = { dialogVisibility.showGenerateYearConfirm.value = true }
    )

    SettingsTimePickerDialogs(
        state = state,
        dialogVisibility = dialogVisibility
    )

    SettingsContentBody(
        state = SettingsContentBodyState(
            uiState = state.uiState,
            actions = guardedActions,
            settingsWorkTypeState = workTypeUi.settingsWorkTypeDialogState,
            settingsTimePickerState = SettingsTimePickerState(
                onDailyWorkTimePickerClick = { dialogVisibility.showWorkTimePicker.value = true },
                onDailyLunchTimeEstimatePickerClick = { dialogVisibility.showLunchTimePicker.value = true }
            ),
            settingsAddWorkTypeDialogState = workTypeUi.settingsAddWorkTypeDialogState,
            scrollState = rememberScrollState(),
            settingsSaveUi = saveUi,
            defaultWorkType = state.defaultWorkType
        )
    )

    SettingsConfirmDialogsSection(
        selectedDate = state.uiState.selectedDate,
        actions = state.actions,
        dialogVisibility = dialogVisibility
    )
}


@Composable
private fun SettingsTimePickerDialogs(
    state: SettingsContentState,
    dialogVisibility: SettingsDialogVisibilityState
) {
    SettingsTimePickerDialogsSection(
        workTimePicker = SettingsTimePickerDialogConfig(
            time = state.uiState.data.dailyWorkTimeEstimate,
            isVisible = dialogVisibility.showWorkTimePicker.value,
            onDismiss = { dialogVisibility.showWorkTimePicker.value = false },
            onConfirm = {
                state.actions.onDailyWorkTimeEstimateChange(it)
                dialogVisibility.showWorkTimePicker.value = false
            }
        ),
        lunchTimePicker = SettingsTimePickerDialogConfig(
            time = state.uiState.data.dailyLunchTimeEstimate,
            isVisible = dialogVisibility.showLunchTimePicker.value,
            onDismiss = { dialogVisibility.showLunchTimePicker.value = false },
            onConfirm = {
                state.actions.onDailyLunchTimeEstimateChange(it)
                dialogVisibility.showLunchTimePicker.value = false
            }
        )
    )
}

@Composable
private fun SettingsUnsavedChangesEffects(
    saveUiHasUnsavedChanges: Boolean,
    onUnsavedChangesChanged: (Boolean) -> Unit,
    registerUnsavedActions: (onSave: (() -> Unit)?, onDiscard: (() -> Unit)?) -> Unit,
    onSaveRequested: () -> Unit,
    onDiscardChanges: () -> Unit
) {
    val latestOnDiscardChanges = rememberUpdatedState(onDiscardChanges)
    val latestOnSaveRequested = rememberUpdatedState(onSaveRequested)

    LaunchedEffect(saveUiHasUnsavedChanges) {
        onUnsavedChangesChanged(saveUiHasUnsavedChanges)
    }

    LaunchedEffect(saveUiHasUnsavedChanges, registerUnsavedActions) {
        registerUnsavedActions(latestOnSaveRequested.value, latestOnDiscardChanges.value)
    }
}

@Composable
private fun SettingsConfirmDialogsSection(
    selectedDate: String,
    actions: SettingsActions,
    dialogVisibility: SettingsDialogVisibilityState
) {
    SettingsGenerateMonthConfirmDialogSection(
        isVisible = dialogVisibility.showGenerateMonthConfirm.value,
        onDismiss = { dialogVisibility.showGenerateMonthConfirm.value = false },
        onConfirmed = {
            dialogVisibility.showGenerateMonthConfirm.value = false
            actions.onGenerateWorkdaysForMonth()
        }
    )

    SettingsGenerateMonthlyReportConfirmDialogSection(
        isVisible = dialogVisibility.showGenerateMonthlyReportConfirm.value,
        selectedDate = selectedDate,
        onDismiss = { dialogVisibility.showGenerateMonthlyReportConfirm.value = false },
        onConfirmed = {
            dialogVisibility.showGenerateMonthlyReportConfirm.value = false
            actions.onGenerateXlsx()
        }
    )

    SettingsGenerateYearConfirmDialogSection(
        isVisible = dialogVisibility.showGenerateYearConfirm.value,
        onDismiss = { dialogVisibility.showGenerateYearConfirm.value = false },
        onConfirmed = {
            dialogVisibility.showGenerateYearConfirm.value = false
            actions.onGenerateWorkdaysForYear()
        }
    )
}

@Composable
private fun SettingsContentBody(
    state: SettingsContentBodyState
) {
    ScrollableScreenColumn(
        state = ScrollableScreenColumnState(
            scrollState = state.scrollState,
            modifier = Modifier.fillMaxSize(),
            columnModifier = Modifier
                .fillMaxWidth()
                .padding(all = FORM_SECTION_SPACING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = SCREEN_CONTENT_SPACING)
        )
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
            state = SettingsActionButtonsSectionState(
                onSave = state.settingsSaveUi.onSaveRequested,
                onGenerateXlsx = state.actions.onGenerateXlsx,
                onGenerateWorkdaysForMonth = state.actions.onGenerateWorkdaysForMonth,
                onGenerateWorkdaysForYear = state.actions.onGenerateWorkdaysForYear,
                isReportEnabled = state.uiState.selectedDate.isNotBlank(),
                isSaveEnabled = state.settingsSaveUi.isSaveEnabled
            )
        )

        SettingsAddWorkTypeDialogSection(
            isVisible = state.settingsAddWorkTypeDialogState.isVisible,
            onDismiss = state.settingsAddWorkTypeDialogState.onDismiss,
            onConfirmed = state.settingsAddWorkTypeDialogState.onConfirm
        )
    }
}

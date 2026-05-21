package com.akiwiksten.awtimesheet.feature.workday.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.core.calculator.WorkTimeCalculator
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.workday.WORK_TIME_BY_DATE_ESTIMATE_INPUT_REGEX
import com.akiwiksten.awtimesheet.feature.workday.WorkdayActions
import com.akiwiksten.awtimesheet.feature.workday.WorkdayHeaderActions
import com.akiwiksten.awtimesheet.feature.workday.WorkdaySettingsEditorState
import com.akiwiksten.awtimesheet.feature.workday.WorkdayUiState

@Composable
internal fun WorkdaySuccessContent(
    state: WorkdayUiState.Success,
    selectedItemIndex: Int,
    actions: WorkdayActions
) {
    val estimateUiState = rememberWorkdayEstimateUiState(
        initialWorkTimeByDateEstimate = state.workTimeByDateEstimate,
        onSaveSettings = actions.onSaveSettings
    )

    val displayState = rememberWorkdayDisplayState(state = state, estimateUiState = estimateUiState)

    WorkdayHeaderSection(date = state.date)
    WorkdayStatsCard(
        state = WorkdayStatsCardState(
            workTime = state.workTimeByDate,
            flexTimeByDate = displayState.displayedFlexTimeByDate,
            calculatedFlexTimeTotal = displayState.displayedCalculatedFlexTimeTotal,
            editorState = displayState.editorState
        ),
        headerActions = displayState.headerActions
    )

    WorkdayListSection(
        items = state.projects,
        selectedIndex = selectedItemIndex,
        onItemSelected = actions.onSelectedItemIndexChange
    )

    WorkdayActionButtonsSection(
        items = state.projects,
        selectedIndex = selectedItemIndex,
        onAddClick = {
            actions.onTrackProjectEditorLaunch(state.flexTimeByDate, state.workTimeByDate)
            actions.onNavigateToSingleProject(SingleProjectState(index = -1, date = state.date))
        },
        onEditClick = {
            state.projects.getOrNull(index = selectedItemIndex)?.let { selectedProject ->
                actions.onTrackProjectEditorLaunch(state.flexTimeByDate, state.workTimeByDate)
                actions.onNavigateToSingleProject(
                    selectedProject.copy(date = selectedProject.date.ifBlank { state.date })
                )
            }
        },
        onDeleteClick = {
            state.projects.getOrNull(index = selectedItemIndex)?.let(actions.onDeleteProject)
        }
    )
}

@Composable
private fun rememberWorkdayDisplayState(
    state: WorkdayUiState.Success,
    estimateUiState: WorkdayEstimateUiState
): WorkdayDisplayState {
    val displayedFlexTimeByDate = remember(
        state.workTimeByDate,
        state.flexTimeByDate,
        estimateUiState.workTimeByDateEstimate,
        estimateUiState.isWorkTimeByDateEstimateValid
    ) {
        if (estimateUiState.isWorkTimeByDateEstimateValid) {
            WorkTimeCalculator.calculateFlexTime(
                initialTime = state.workTimeByDate,
                addedTime = "-${estimateUiState.workTimeByDateEstimate}"
            )
        } else {
            state.flexTimeByDate
        }
    }

    val displayedCalculatedFlexTimeTotal = remember(
        state.initialFlexTimeTotal,
        state.flexTimeTotal,
        state.flexTimeByDate,
        displayedFlexTimeByDate
    ) {
        calculateDisplayedCalculatedFlexTimeTotal(
            persistedInitialFlexTimeTotal = state.initialFlexTimeTotal,
            persistedDisplayedFlexTimeTotal = state.flexTimeTotal,
            persistedFlexTimeByDate = state.flexTimeByDate,
            editedFlexTimeByDate = displayedFlexTimeByDate
        )
    }

    return WorkdayDisplayState(
        displayedFlexTimeByDate = displayedFlexTimeByDate,
        displayedCalculatedFlexTimeTotal = displayedCalculatedFlexTimeTotal,
        editorState = WorkdaySettingsEditorState(
            workTimeByDateEstimate = estimateUiState.workTimeByDateEstimate,
            isWorkTimeByDateEstimateError = !estimateUiState.isWorkTimeByDateEstimateValid
        ),
        headerActions = WorkdayHeaderActions(
            onWorkTimeByDateEstimateChange = estimateUiState.onWorkTimeByDateEstimateChange
        )
    )
}

internal fun calculateDisplayedCalculatedFlexTimeTotal(
    persistedInitialFlexTimeTotal: String,
    persistedDisplayedFlexTimeTotal: String,
    persistedFlexTimeByDate: String,
    editedFlexTimeByDate: String
): String {
    val persistedCalculatedFlexDeltaTotal = WorkTimeCalculator.calculateFlexTime(
        initialTime = persistedDisplayedFlexTimeTotal,
        addedTime = WorkTimeCalculator.normalizeDuplicateMinus("-$persistedInitialFlexTimeTotal")
    )

    val flexTimeByDateDelta = WorkTimeCalculator.calculateFlexTime(
        initialTime = editedFlexTimeByDate,
        addedTime = WorkTimeCalculator.normalizeDuplicateMinus("-$persistedFlexTimeByDate")
    )

    val recalculatedFlexDeltaTotal = WorkTimeCalculator.calculateFlexTime(
        initialTime = persistedCalculatedFlexDeltaTotal,
        addedTime = flexTimeByDateDelta
    )

    return WorkTimeCalculator.calculateFlexTime(
        initialTime = persistedInitialFlexTimeTotal,
        addedTime = recalculatedFlexDeltaTotal
    )
}

@Suppress("kotlin:S6615", "UNUSED_VALUE")
@Composable
private fun rememberWorkdayEstimateUiState(
    initialWorkTimeByDateEstimate: String,
    onSaveSettings: (String, Boolean) -> Unit
): WorkdayEstimateUiState {
    val context = LocalContext.current
    val globalSavedText = stringResource(id = R.string.saved_globally)
    val todaySavedText = stringResource(id = R.string.saved_today)
    var editedWorkTimeByDateEstimate by remember(initialWorkTimeByDateEstimate) {
        mutableStateOf(value = initialWorkTimeByDateEstimate)
    }
    val showSaveDialogState = remember { mutableStateOf(false) }
    val pendingWorkTimeByDateEstimateState = remember { mutableStateOf("") }

    val isWorkTimeByDateEstimateValid = remember(editedWorkTimeByDateEstimate) {
        editedWorkTimeByDateEstimate.matches(WORK_TIME_BY_DATE_ESTIMATE_INPUT_REGEX)
    }

    if (showSaveDialogState.value) {
        SaveWorkTimeEstimateDialog(
            onDismiss = { showSaveDialogState.value = false },
            onSaveToday = {
                showSaveDialogState.value = false
                onSaveSettings(pendingWorkTimeByDateEstimateState.value, false)
                Toast.makeText(context, todaySavedText, Toast.LENGTH_SHORT).show()
            },
            onSaveGlobally = {
                showSaveDialogState.value = false
                onSaveSettings(pendingWorkTimeByDateEstimateState.value, true)
                Toast.makeText(context, globalSavedText, Toast.LENGTH_SHORT).show()
            }
        )
    }

    return WorkdayEstimateUiState(
        workTimeByDateEstimate = editedWorkTimeByDateEstimate,
        isWorkTimeByDateEstimateValid = isWorkTimeByDateEstimateValid,
        onWorkTimeByDateEstimateChange = { value ->
            editedWorkTimeByDateEstimate = value
            if (value != initialWorkTimeByDateEstimate && value.matches(WORK_TIME_BY_DATE_ESTIMATE_INPUT_REGEX)) {
                pendingWorkTimeByDateEstimateState.value = value
                showSaveDialogState.value = true
            }
        }
    )
}

@Composable
private fun SaveWorkTimeEstimateDialog(
    onDismiss: () -> Unit,
    onSaveToday: () -> Unit,
    onSaveGlobally: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { androidx.compose.material3.Text(text = stringResource(id = R.string.save_work_time_estimate)) },
        text = {
            androidx.compose.material3.Text(
                text = stringResource(id = R.string.save_work_time_estimate_message),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = onSaveGlobally) {
                androidx.compose.material3.Text(text = stringResource(id = R.string.save_globally))
            }
        },
        dismissButton = {
            androidx.compose.material3.Button(onClick = onSaveToday) {
                androidx.compose.material3.Text(text = stringResource(id = R.string.save_today))
            }
        }
    )
}

private data class WorkdayEstimateUiState(
    val workTimeByDateEstimate: String,
    val isWorkTimeByDateEstimateValid: Boolean,
    val onWorkTimeByDateEstimateChange: (String) -> Unit
)

private data class WorkdayDisplayState(
    val displayedFlexTimeByDate: String,
    val displayedCalculatedFlexTimeTotal: String,
    val editorState: WorkdaySettingsEditorState,
    val headerActions: WorkdayHeaderActions
)




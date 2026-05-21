package com.akiwiksten.awtimesheet.feature.workday.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.core.calculator.WorkTimeCalculator
import com.akiwiksten.awtimesheet.core.ui.CenteredLoadingBox
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.workday.SettingsEditorState
import com.akiwiksten.awtimesheet.feature.workday.WORK_TIME_BY_DATE_ESTIMATE_INPUT_REGEX
import com.akiwiksten.awtimesheet.feature.workday.WorkdayActions
import com.akiwiksten.awtimesheet.feature.workday.WorkdayHeaderActions
import com.akiwiksten.awtimesheet.feature.workday.WorkdayUiState

@Composable
internal fun WorkdayLoadingContent(
    showLoadingIndicator: Boolean,
    cachedState: WorkdayUiState.Success?,
    workTimeByDateChange: String,
    selectedItemIndex: Int,
    actions: WorkdayActions
) {
    if (showLoadingIndicator) {
        CenteredLoadingBox()
        return
    }

    cachedState?.let {
        WorkdaySuccessContent(
            state = it,
            workTimeByDateChange = workTimeByDateChange,
            selectedItemIndex = selectedItemIndex,
            actions = actions
        )
    }
}

@Composable
internal fun WorkdaySuccessContent(
    state: WorkdayUiState.Success,
    workTimeByDateChange: String,
    selectedItemIndex: Int,
    actions: WorkdayActions
) {
    val estimateUiState = rememberWorkdayEstimateUiState(
        initialWorkTimeByDateEstimate = state.workTimeByDateEstimate,
        onSaveSettings = actions.onSaveSettings
    )

    val displayState = rememberWorkdayDisplayState(state = state, estimateUiState = estimateUiState)

    WorkdayHeader(
        date = state.date
    )
    WorkdayStatsCard(
        state = WorkdayStatsCardState(
            workTime = state.workTimeByDate,
            flexTimeByDate = displayState.displayedFlexTimeByDate,
            calculatedFlexTimeTotal = displayState.displayedCalculatedFlexTimeTotal,
            workTimeByDateChange = workTimeByDateChange,
            settingsEditorState = displayState.settingsEditorState
        ),
        headerActions = displayState.headerActions
    )

    WorkdayListSection(
        items = state.projects,
        selectedIndex = selectedItemIndex,
        onItemSelected = actions.onSelectedItemIndexChange,
        modifier = Modifier.fillMaxWidth()
    )

    WorkdayActionButtons(
        items = state.projects,
        selectedIndex = selectedItemIndex,
        onAddClick = {
            actions.onNavigateToSingleProject(
                SingleProjectState(index = -1, date = state.date)
            )
        },
        onEditClick = {
            state.projects.getOrNull(index = selectedItemIndex)?.let { selectedProject ->
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
            persistedCalculatedFlexTimeTotal = state.flexTimeTotal,
            persistedFlexTimeByDate = state.flexTimeByDate,
            editedFlexTimeByDate = displayedFlexTimeByDate
        )
    }

    return WorkdayDisplayState(
        displayedFlexTimeByDate = displayedFlexTimeByDate,
        displayedCalculatedFlexTimeTotal = displayedCalculatedFlexTimeTotal,
        settingsEditorState = SettingsEditorState(
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
    persistedCalculatedFlexTimeTotal: String,
    persistedFlexTimeByDate: String,
    editedFlexTimeByDate: String
): String {
    val calculatedOnlyFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
        initialTime = persistedCalculatedFlexTimeTotal,
        addedTime = WorkTimeCalculator.normalizeDuplicateMinus("-$persistedInitialFlexTimeTotal")
    )

    val flexTimeByDateDelta = WorkTimeCalculator.calculateFlexTime(
        initialTime = editedFlexTimeByDate,
        addedTime = WorkTimeCalculator.normalizeDuplicateMinus("-$persistedFlexTimeByDate")
    )

    val recalculatedOnlyFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
        initialTime = calculatedOnlyFlexTimeTotal,
        addedTime = flexTimeByDateDelta
    )

    return WorkTimeCalculator.calculateFlexTime(
        initialTime = persistedInitialFlexTimeTotal,
        addedTime = recalculatedOnlyFlexTimeTotal
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.save_work_time_estimate)) },
        text = {
            Text(
                text = stringResource(id = R.string.save_work_time_estimate_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onSaveGlobally) {
                Text(text = stringResource(id = R.string.save_globally))
            }
        },
        dismissButton = {
            Button(onClick = onSaveToday) {
                Text(text = stringResource(id = R.string.save_today))
            }
        }
    )
}

@Composable
internal fun WorkdayErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error: $message",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(all = 32.dp)
        )
        Button(onClick = onRetry) {
            Text(text = stringResource(id = R.string.retry))
        }
    }
}

private data class WorkdayEstimateUiState(
    val workTimeByDateEstimate: String,
    val isWorkTimeByDateEstimateValid: Boolean,
    val onWorkTimeByDateEstimateChange: (String) -> Unit
)

private data class WorkdayDisplayState(
    val displayedFlexTimeByDate: String,
    val displayedCalculatedFlexTimeTotal: String,
    val settingsEditorState: SettingsEditorState,
    val headerActions: WorkdayHeaderActions
)

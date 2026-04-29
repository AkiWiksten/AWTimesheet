package com.akiwiksten.worktime30.feature.workday.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.feature.workday.SettingsEditorState
import com.akiwiksten.worktime30.feature.workday.WORK_TIME_TODAY_ESTIMATE_INPUT_REGEX
import com.akiwiksten.worktime30.feature.workday.WorkdayActions
import com.akiwiksten.worktime30.feature.workday.WorkdayHeaderActions
import com.akiwiksten.worktime30.feature.workday.WorkdayUiState

@Composable
internal fun ColumnScope.WorkdayLoadingContent(
    showLoadingIndicator: Boolean,
    cachedState: WorkdayUiState.Success?,
    selectedItemIndex: Int,
    actions: WorkdayActions
) {
    if (showLoadingIndicator) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    cachedState?.let {
        WorkdaySuccessContent(state = it, selectedItemIndex = selectedItemIndex, actions = actions)
    }
}

@Composable
internal fun ColumnScope.WorkdaySuccessContent(
    state: WorkdayUiState.Success,
    selectedItemIndex: Int,
    actions: WorkdayActions
) {
    val saveUi = rememberWorkdaySaveUi(
        initialWorkTimeTodayEstimate = state.workTimeTodayEstimate,
        onSaveSettings = actions.onSaveSettings
    )

    val displayState = rememberWorkdayDisplayState(state = state, saveUi = saveUi)

    WorkdayHeader(
        date = state.date
    )
    WorkdayStatsCard(
        workTime = state.workTimeToday,
        flexTimeToday = displayState.displayedFlexTimeToday,
        calculatedFlexTimeTotal = displayState.displayedCalculatedFlexTimeTotal,
        settingsEditorState = displayState.settingsEditorState,
        headerActions = displayState.headerActions
    )

    WorkdayListSection(
        items = state.projects,
        selectedIndex = selectedItemIndex,
        onItemSelected = actions.onSelectedItemIndexChange,
        modifier = Modifier.weight(weight = 1f)
    )

    WorkdayActionButtons(
        items = state.projects,
        selectedIndex = selectedItemIndex,
        onAddClick = { actions.onNavigateToSingleProject(-1) },
        onEditClick = { actions.onNavigateToSingleProject(selectedItemIndex) },
        onDeleteClick = {
            state.projects.getOrNull(index = selectedItemIndex)?.let(actions.onDeleteProject)
        }
    )
}

@Composable
private fun rememberWorkdayDisplayState(
    state: WorkdayUiState.Success,
    saveUi: WorkdaySaveUi
): WorkdayDisplayState {
    val displayedFlexTimeToday = remember(
        state.workTimeToday,
        state.flexTimeToday,
        saveUi.workTimeTodayEstimate,
        saveUi.isWorkTimeTodayEstimateValid
    ) {
        if (saveUi.isWorkTimeTodayEstimateValid) {
            WorkTimeCalculator.calculateFlexTime(
                initialTime = state.workTimeToday,
                addedTime = "-${saveUi.workTimeTodayEstimate}"
            )
        } else {
            state.flexTimeToday
        }
    }

    val displayedCalculatedFlexTimeTotal = remember(
        state.initialFlexTimeTotal,
        state.calculatedFlexTimeTotal,
        state.flexTimeToday,
        displayedFlexTimeToday
    ) {
        calculateDisplayedCalculatedFlexTimeTotal(
            persistedInitialFlexTimeTotal = state.initialFlexTimeTotal,
            persistedCalculatedFlexTimeTotal = state.calculatedFlexTimeTotal,
            persistedFlexTimeToday = state.flexTimeToday,
            editedFlexTimeToday = displayedFlexTimeToday
        )
    }

    return WorkdayDisplayState(
        displayedFlexTimeToday = displayedFlexTimeToday,
        displayedCalculatedFlexTimeTotal = displayedCalculatedFlexTimeTotal,
        settingsEditorState = SettingsEditorState(
            workTimeTodayEstimate = saveUi.workTimeTodayEstimate,
            isWorkTimeTodayEstimateError = !saveUi.isWorkTimeTodayEstimateValid
        ),
        headerActions = WorkdayHeaderActions(
            onWorkTimeTodayEstimateChange = saveUi.onWorkTimeTodayEstimateChange
        )
    )
}

internal fun calculateDisplayedCalculatedFlexTimeTotal(
    persistedInitialFlexTimeTotal: String,
    persistedCalculatedFlexTimeTotal: String,
    persistedFlexTimeToday: String,
    editedFlexTimeToday: String
): String {
    val calculatedOnlyFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
        initialTime = persistedCalculatedFlexTimeTotal,
        addedTime = WorkTimeCalculator.checkIfDoubleMinus("-$persistedInitialFlexTimeTotal")
    )

    val flexTimeTodayDelta = WorkTimeCalculator.calculateFlexTime(
        initialTime = editedFlexTimeToday,
        addedTime = WorkTimeCalculator.checkIfDoubleMinus("-$persistedFlexTimeToday")
    )

    val recalculatedOnlyFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
        initialTime = calculatedOnlyFlexTimeTotal,
        addedTime = flexTimeTodayDelta
    )

    return WorkTimeCalculator.calculateFlexTime(
        initialTime = persistedInitialFlexTimeTotal,
        addedTime = recalculatedOnlyFlexTimeTotal
    )
}

@Composable
private fun rememberWorkdaySaveUi(
    initialWorkTimeTodayEstimate: String,
    onSaveSettings: (String, Boolean) -> Unit
): WorkdaySaveUi {
    val context = LocalContext.current
    val globalSavedText = stringResource(id = R.string.saved_globally)
    val todaySavedText = stringResource(id = R.string.saved_today)
    var editedWorkTimeTodayEstimate by remember(initialWorkTimeTodayEstimate) {
        mutableStateOf(value = initialWorkTimeTodayEstimate)
    }
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingWorkTimeTodayEstimate by remember { mutableStateOf("") }

    val isWorkTimeTodayEstimateValid = remember(editedWorkTimeTodayEstimate) {
        editedWorkTimeTodayEstimate.matches(WORK_TIME_TODAY_ESTIMATE_INPUT_REGEX)
    }

    if (showSaveDialog) {
        SaveWorkTimeEstimateDialog(
            onDismiss = { showSaveDialog = false },
            onSaveToday = {
                showSaveDialog = false
                onSaveSettings(pendingWorkTimeTodayEstimate, false)
                Toast.makeText(context, todaySavedText, Toast.LENGTH_SHORT).show()
            },
            onSaveGlobally = {
                showSaveDialog = false
                onSaveSettings(pendingWorkTimeTodayEstimate, true)
                Toast.makeText(context, globalSavedText, Toast.LENGTH_SHORT).show()
            }
        )
    }

    return WorkdaySaveUi(
        workTimeTodayEstimate = editedWorkTimeTodayEstimate,
        isWorkTimeTodayEstimateValid = isWorkTimeTodayEstimateValid,
        onWorkTimeTodayEstimateChange = { value ->
            editedWorkTimeTodayEstimate = value
            if (value != initialWorkTimeTodayEstimate && value.matches(WORK_TIME_TODAY_ESTIMATE_INPUT_REGEX)) {
                pendingWorkTimeTodayEstimate = value
                showSaveDialog = true
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

private data class WorkdaySaveUi(
    val workTimeTodayEstimate: String,
    val isWorkTimeTodayEstimateValid: Boolean,
    val onWorkTimeTodayEstimateChange: (String) -> Unit
)

private data class WorkdayDisplayState(
    val displayedFlexTimeToday: String,
    val displayedCalculatedFlexTimeTotal: String,
    val settingsEditorState: SettingsEditorState,
    val headerActions: WorkdayHeaderActions
)

package com.akiwiksten.worktime30.feature.workday.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.akiwiksten.worktime30.core.ui.hasChanges
import com.akiwiksten.worktime30.core.ui.isActionEnabled
import com.akiwiksten.worktime30.feature.workday.INITIAL_FLEX_TIME_TOTAL_INPUT_REGEX
import com.akiwiksten.worktime30.feature.workday.WORK_TIME_TODAY_ESTIMATE_INPUT_REGEX
import com.akiwiksten.worktime30.feature.workday.WorkStatsEditorState
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
        initialFlexTimeTotal = state.initialFlexTimeTotal,
        onSaveWorkStats = actions.onSaveWorkStats
    )
    val displayedCalculatedFlexTimeTotal = remember(
        state.initialFlexTimeTotal,
        state.calculatedFlexTimeTotal,
        saveUi.initialFlexTimeTotal
    ) {
        calculateDisplayedCalculatedFlexTimeTotal(
            persistedInitialFlexTimeTotal = state.initialFlexTimeTotal,
            persistedCalculatedFlexTimeTotal = state.calculatedFlexTimeTotal,
            editedInitialFlexTimeTotal = saveUi.initialFlexTimeTotal
        )
    }
    val workStatsEditorState = WorkStatsEditorState(
        workTimeTodayEstimate = saveUi.workTimeTodayEstimate,
        initialFlexTimeTotal = saveUi.initialFlexTimeTotal,
        isWorkTimeTodayEstimateError = !saveUi.isWorkTimeTodayEstimateValid,
        isInitialFlexTimeTotalError = !saveUi.isInitialFlexTimeTotalValid,
        hasUnsavedChanges = saveUi.hasUnsavedChanges
    )
    val headerActions = WorkdayHeaderActions(
        onWorkTimeTodayEstimateChange = saveUi.onWorkTimeTodayEstimateChange,
        onInitialFlexTimeTotalChange = saveUi.onInitialFlexTimeTotalChange,
        onSaveWorkStats = saveUi.onSaveRequested
    )

    WorkdayHeader(
        date = state.date
    )
    WorkdayStatsCard(
        workTime = state.workTimeToday,
        flexTimeToday = state.flexTimeToday,
        calculatedFlexTimeTotal = displayedCalculatedFlexTimeTotal,
        workStatsEditorState = workStatsEditorState,
        headerActions = headerActions
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

internal fun calculateDisplayedCalculatedFlexTimeTotal(
    persistedInitialFlexTimeTotal: String,
    persistedCalculatedFlexTimeTotal: String,
    editedInitialFlexTimeTotal: String
): String {
    val calculatedOnlyFlexTimeTotal = WorkTimeCalculator.calculateFlexTime(
        initialTime = persistedCalculatedFlexTimeTotal,
        addedTime = WorkTimeCalculator.checkIfDoubleMinus("-$persistedInitialFlexTimeTotal")
    )

    return WorkTimeCalculator.calculateFlexTime(
        initialTime = editedInitialFlexTimeTotal,
        addedTime = calculatedOnlyFlexTimeTotal
    )
}

@Composable
private fun rememberWorkdaySaveUi(
    initialWorkTimeTodayEstimate: String,
    initialFlexTimeTotal: String,
    onSaveWorkStats: (String, String) -> Unit
): WorkdaySaveUi {
    val context = LocalContext.current
    val savedText = stringResource(id = R.string.saved)
    var editedWorkTimeTodayEstimate by remember(initialWorkTimeTodayEstimate) {
        mutableStateOf(value = initialWorkTimeTodayEstimate)
    }
    var editedInitialFlexTimeTotal by remember(initialFlexTimeTotal) {
        mutableStateOf(value = initialFlexTimeTotal)
    }
    val lastSavedWorkTimeTodayEstimateState = remember(initialWorkTimeTodayEstimate) {
        mutableStateOf(value = initialWorkTimeTodayEstimate)
    }
    val lastSavedInitialFlexTimeTotalState = remember(initialFlexTimeTotal) {
        mutableStateOf(value = initialFlexTimeTotal)
    }

    val isWorkTimeTodayEstimateValid = remember(editedWorkTimeTodayEstimate) {
        editedWorkTimeTodayEstimate.matches(WORK_TIME_TODAY_ESTIMATE_INPUT_REGEX)
    }
    val isInitialFlexTimeTotalValid = remember(editedInitialFlexTimeTotal) {
        editedInitialFlexTimeTotal.matches(INITIAL_FLEX_TIME_TOTAL_INPUT_REGEX)
    }
    val hasUnsavedChanges =
        hasChanges(current = editedWorkTimeTodayEstimate, baseline = lastSavedWorkTimeTodayEstimateState.value) ||
            hasChanges(current = editedInitialFlexTimeTotal, baseline = lastSavedInitialFlexTimeTotalState.value)
    val isSaveEnabled = isActionEnabled(
        hasRequiredFields = isWorkTimeTodayEstimateValid && isInitialFlexTimeTotalValid,
        hasUnsavedChanges = hasUnsavedChanges
    )

    return WorkdaySaveUi(
        workTimeTodayEstimate = editedWorkTimeTodayEstimate,
        initialFlexTimeTotal = editedInitialFlexTimeTotal,
        isWorkTimeTodayEstimateValid = isWorkTimeTodayEstimateValid,
        isInitialFlexTimeTotalValid = isInitialFlexTimeTotalValid,
        hasUnsavedChanges = hasUnsavedChanges,
        onWorkTimeTodayEstimateChange = { editedWorkTimeTodayEstimate = it },
        onInitialFlexTimeTotalChange = { editedInitialFlexTimeTotal = it },
        onSaveRequested = {
            if (isSaveEnabled) {
                onSaveWorkStats(editedWorkTimeTodayEstimate, editedInitialFlexTimeTotal)
                lastSavedWorkTimeTodayEstimateState.value = editedWorkTimeTodayEstimate
                lastSavedInitialFlexTimeTotalState.value = editedInitialFlexTimeTotal
                Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
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
    val initialFlexTimeTotal: String,
    val isWorkTimeTodayEstimateValid: Boolean,
    val isInitialFlexTimeTotalValid: Boolean,
    val hasUnsavedChanges: Boolean,
    val onWorkTimeTodayEstimateChange: (String) -> Unit,
    val onInitialFlexTimeTotalChange: (String) -> Unit,
    val onSaveRequested: () -> Unit
)

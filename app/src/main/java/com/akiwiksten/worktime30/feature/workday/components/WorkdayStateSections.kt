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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.hasChanges
import com.akiwiksten.worktime30.core.ui.isActionEnabled
import com.akiwiksten.worktime30.feature.workday.DAILY_WORK_TIME_INPUT_REGEX
import com.akiwiksten.worktime30.feature.workday.FLEX_TIME_TOTAL_INPUT_REGEX
import com.akiwiksten.worktime30.feature.workday.WorkdayActions
import com.akiwiksten.worktime30.feature.workday.WorkdayHeaderActions
import com.akiwiksten.worktime30.feature.workday.WorkdayUiState
import com.akiwiksten.worktime30.feature.workday.WorkStatsEditorState

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
        initialDailyWorkTime = state.dailyWorkTime,
        initialFlexTimeTotal = state.flexTimeTotal,
        onSaveWorkStats = actions.onSaveWorkStats
    )
    val workStatsEditorState = WorkStatsEditorState(
        dailyWorkTime = saveUi.dailyWorkTime,
        flexTimeTotal = saveUi.flexTimeTotal,
        isDailyWorkTimeError = !saveUi.isDailyWorkTimeValid,
        isFlexTimeTotalError = !saveUi.isFlexTimeTotalValid,
        hasUnsavedChanges = saveUi.hasUnsavedChanges
    )
    val headerActions = WorkdayHeaderActions(
        onDailyWorkTimeChange = saveUi.onDailyWorkTimeChange,
        onFlexTimeTotalChange = saveUi.onFlexTimeTotalChange,
        onSaveWorkStats = saveUi.onSaveRequested
    )

    WorkdayHeader(
        date = state.date
    )
    WorkdayStatsCard(
        workTime = state.workTimeToday,
        flexTimeToday = state.flexTimeToday,
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

@Composable
private fun rememberWorkdaySaveUi(
    initialDailyWorkTime: String,
    initialFlexTimeTotal: String,
    onSaveWorkStats: (String, String) -> Unit
): WorkdaySaveUi {
    val context = LocalContext.current
    val savedText = stringResource(id = R.string.saved)
    var editedDailyWorkTime by remember(initialDailyWorkTime) {
        mutableStateOf(value = initialDailyWorkTime)
    }
    var editedFlexTimeTotal by remember(initialFlexTimeTotal) {
        mutableStateOf(value = initialFlexTimeTotal)
    }
    val lastSavedDailyWorkTimeState = remember(initialDailyWorkTime) {
        mutableStateOf(value = initialDailyWorkTime)
    }
    val lastSavedFlexTimeTotalState = remember(initialFlexTimeTotal) {
        mutableStateOf(value = initialFlexTimeTotal)
    }

    val isDailyWorkTimeValid = remember(editedDailyWorkTime) {
        editedDailyWorkTime.matches(DAILY_WORK_TIME_INPUT_REGEX)
    }
    val isFlexTimeTotalValid = remember(editedFlexTimeTotal) {
        editedFlexTimeTotal.matches(FLEX_TIME_TOTAL_INPUT_REGEX)
    }
    val hasUnsavedChanges =
        hasChanges(current = editedDailyWorkTime, baseline = lastSavedDailyWorkTimeState.value) ||
            hasChanges(current = editedFlexTimeTotal, baseline = lastSavedFlexTimeTotalState.value)
    val isSaveEnabled = isActionEnabled(
        hasRequiredFields = isDailyWorkTimeValid && isFlexTimeTotalValid,
        hasUnsavedChanges = hasUnsavedChanges
    )

    return WorkdaySaveUi(
        dailyWorkTime = editedDailyWorkTime,
        flexTimeTotal = editedFlexTimeTotal,
        isDailyWorkTimeValid = isDailyWorkTimeValid,
        isFlexTimeTotalValid = isFlexTimeTotalValid,
        hasUnsavedChanges = hasUnsavedChanges,
        onDailyWorkTimeChange = { editedDailyWorkTime = it },
        onFlexTimeTotalChange = { editedFlexTimeTotal = it },
        onSaveRequested = {
            if (isSaveEnabled) {
                onSaveWorkStats(editedDailyWorkTime, editedFlexTimeTotal)
                lastSavedDailyWorkTimeState.value = editedDailyWorkTime
                lastSavedFlexTimeTotalState.value = editedFlexTimeTotal
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
    val dailyWorkTime: String,
    val flexTimeTotal: String,
    val isDailyWorkTimeValid: Boolean,
    val isFlexTimeTotalValid: Boolean,
    val hasUnsavedChanges: Boolean,
    val onDailyWorkTimeChange: (String) -> Unit,
    val onFlexTimeTotalChange: (String) -> Unit,
    val onSaveRequested: () -> Unit
)



package com.akiwiksten.awtimesheet.feature.workday.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.WorkTimeDisplayCalculator
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.ui.CenteredLoadingBox
import com.akiwiksten.awtimesheet.core.ui.LocalContentBottomPadding
import com.akiwiksten.awtimesheet.core.ui.NoteBanner
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.model.isProjectNameOnlyPlaceholder
import com.akiwiksten.awtimesheet.feature.workday.R
import com.akiwiksten.awtimesheet.feature.workday.model.WORK_TIME_BY_DATE_ESTIMATE_INPUT_REGEX
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayActionButtonsState
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayActions
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayDisplayState
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayEstimateUiState
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayHeaderActions
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayListItemUiModel
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdaySettingsEditorState
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayStatsCardState
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayUiState

@Composable
internal fun WorkdayLoadingContent(
    showLoadingIndicator: Boolean,
    cachedState: WorkdayUiState.Success?,
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
            selectedItemIndex = selectedItemIndex,
            actions = actions
        )
    }
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
            modifier = Modifier.padding(all = PADDING_SPACING)
        )
        Button(onClick = onRetry) {
            Text(text = stringResource(id = R.string.retry))
        }
    }
}

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
    val listItems = remember(state.projects) {
        state.projects
            .sortedWith(
                compareBy<SingleProjectState> { it.projectTime == ZERO_TIME }
                    .thenBy { it.projectName }
            )
            .map { it.toListItemUiModel() }
    }
    val selectedItemKey = remember(state.projects, selectedItemIndex) {
        state.projects.firstOrNull { it.index == selectedItemIndex }?.stableListItemKey()
    }

    WorkdayHeaderSection(date = state.date)

    if (state.isFlexTimeByDateSpecialRuleApplied) {
        NoteBanner(text = stringResource(id = R.string.flex_day_special_note))
    }

    WorkdayStatsSection(
        state = WorkdayStatsCardState(
            workTime = state.workTimeByDate,
            flexTimeByDate = displayState.displayedFlexTimeByDate,
            calculatedFlexTimeTotal = displayState.displayedCalculatedFlexTimeTotal,
            editorState = displayState.editorState,
            isTimePickerEnabled = !state.isFlexTimeByDateSpecialRuleApplied
        ),
        headerActions = displayState.headerActions
    )

    WorkdayListSection(
        items = listItems,
        selectedItemKey = selectedItemKey,
        onItemSelected = actions.onSelectedItemIndexChange
    )

    WorkdayActionButtonsSection(
        state = WorkdayActionButtonsState(
            items = state.projects,
            selectedIndex = selectedItemIndex,
            isAddEditDisabled = state.isFlexTimeByDateSpecialRuleApplied
        ),
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

    Spacer(modifier = Modifier.padding(bottom = LocalContentBottomPadding.current))
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
        WorkTimeDisplayCalculator.calculateDisplayedFlexTimeByDate(
            persistedWorkTimeByDate = state.workTimeByDate,
            persistedFlexTimeByDate = state.flexTimeByDate,
            editedWorkTimeByDateEstimate = estimateUiState.workTimeByDateEstimate,
            isEditedWorkTimeByDateEstimateValid = estimateUiState.isWorkTimeByDateEstimateValid,
            usePersistedFlexTimeByDate = state.isFlexTimeByDateSpecialRuleApplied
        )
    }

    val displayedCalculatedFlexTimeTotal = remember(
        state.initialFlexTimeTotal,
        state.flexTimeTotal,
        state.flexTimeByDate,
        displayedFlexTimeByDate
    ) {
        WorkTimeDisplayCalculator.calculateDisplayedCalculatedFlexTimeTotal(
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

private fun SingleProjectState.toListItemUiModel(): WorkdayListItemUiModel {
    return WorkdayListItemUiModel(
        index = index,
        projectName = projectName,
        projectTime = projectTime,
        kilometres = kilometres,
        allowance = allowance,
        workType = workType,
        kilometresLabel = "$kilometres km",
        isProjectNameOnlyPlaceholder = isProjectNameOnlyPlaceholder(),
        stableKey = stableListItemKey()
    )
}

private fun SingleProjectState.stableListItemKey(): String {
    val datePart = if (date.isNotBlank()) date else "<no-date>"
    return "$datePart|index:$index|$projectName|$projectTime|$kilometres|$allowance|$workType"
}

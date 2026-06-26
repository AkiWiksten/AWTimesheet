@file:Suppress("FunctionNaming")

package com.akiwiksten.awtimesheet.feature.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.awtimesheet.core.DEFAULT_WORK_TYPES
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.ui.CenteredErrorBox
import com.akiwiksten.awtimesheet.core.ui.CenteredLoadingBox
import com.akiwiksten.awtimesheet.core.rememberDelayedLoadingVisibility
import com.akiwiksten.awtimesheet.domain.usecase.GeneratedAllowanceLabels
import com.akiwiksten.awtimesheet.domain.usecase.WorkdayGenerationScope
import com.akiwiksten.awtimesheet.feature.settings.components.SettingsContent
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsActions
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsContentState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsLoadingContentState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsStateContentState
import com.akiwiksten.awtimesheet.feature.settings.remember.rememberGeneratedAllowanceLabels
import kotlinx.coroutines.flow.collectLatest
import com.akiwiksten.awtimesheet.core.R as CoreR

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onUnsavedChangesChanged: (Boolean) -> Unit = {},
    registerUnsavedActions: (onSave: (() -> Unit)?, onDiscard: (() -> Unit)?) -> Unit = { _, _ -> }
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val ctx = LocalContext.current
    val defaultWorkTypes = DEFAULT_WORK_TYPES.map { stringResource(id = it) }
    val generatedAllowanceLabels = rememberGeneratedAllowanceLabels()

    SettingsScreenEffects(
        settingsViewModel = settingsViewModel,
        uiState = uiState,
        defaultWorkTypes = defaultWorkTypes,
        ctx = ctx
    )

    SettingsStateContent(
        state = SettingsStateContentState(
            uiState = uiState,
            defaultWorkTypes = defaultWorkTypes,
            onUnsavedChangesChanged = onUnsavedChangesChanged,
            registerUnsavedActions = registerUnsavedActions,
            onDiscardChanges = settingsViewModel::loadSettings,
            createActions = { successState ->
                createSettingsActions(
                    settingsViewModel = settingsViewModel,
                    successState = successState,
                    generatedAllowanceLabels = generatedAllowanceLabels
                )
            }
        )
    )
}

@Composable
internal fun SettingsStateContent(
    state: SettingsStateContentState
) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = state.uiState is SettingsUiState.Loading
    )
    var lastSuccessState by remember { mutableStateOf<SettingsUiState.Success?>(value = null) }

    LaunchedEffect(state.uiState) {
        if (state.uiState is SettingsUiState.Success) {
            lastSuccessState = state.uiState
        }
    }

    when (state.uiState) {
        is SettingsUiState.Loading -> SettingsLoadingContent(
            state = SettingsLoadingContentState(
                showLoadingIndicator = showLoadingIndicator,
                lastSuccessState = lastSuccessState,
                defaultWorkTypes = state.defaultWorkTypes,
                onUnsavedChangesChanged = state.onUnsavedChangesChanged,
                registerUnsavedActions = state.registerUnsavedActions,
                onDiscardChanges = state.onDiscardChanges,
                createActions = state.createActions
            )
        )
        is SettingsUiState.Success -> {
            val actions = remember(state.uiState) { state.createActions(state.uiState) }
            SettingsContent(
                state = SettingsContentState(
                    uiState = state.uiState,
                    actions = actions,
                    defaultWorkTypes = state.defaultWorkTypes,
                    onUnsavedChangesChanged = state.onUnsavedChangesChanged,
                    registerUnsavedActions = state.registerUnsavedActions,
                    onDiscardChanges = state.onDiscardChanges
                )
            )
        }
        is SettingsUiState.Error -> {
            LaunchedEffect(Unit) {
                state.onUnsavedChangesChanged(false)
                state.registerUnsavedActions(null, null)
            }
            CenteredErrorBox(
                errorMessage = stringResource(id = CoreR.string.error_message, state.uiState.message),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = PADDING_SPACING),
                fillMaxSize = false
            )
        }
    }
}

@Composable
private fun SettingsLoadingContent(
    state: SettingsLoadingContentState
) {
    if (state.showLoadingIndicator) {
        CenteredLoadingBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = PADDING_SPACING),
            fillMaxSize = false
        )
    } else if (state.lastSuccessState != null) {
        val actions = remember(state.lastSuccessState) { state.createActions(state.lastSuccessState) }
        SettingsContent(
            state = SettingsContentState(
                uiState = state.lastSuccessState,
                actions = actions,
                defaultWorkTypes = state.defaultWorkTypes,
                onUnsavedChangesChanged = state.onUnsavedChangesChanged,
                registerUnsavedActions = state.registerUnsavedActions,
                onDiscardChanges = state.onDiscardChanges
            )
        )
    } else {
        LaunchedEffect(Unit) {
            state.onUnsavedChangesChanged(false)
            state.registerUnsavedActions(null, null)
        }
        Box(modifier = Modifier.fillMaxSize())
    }
}

@Composable
internal fun SettingsScreenEffects(
    settingsViewModel: SettingsViewModel,
    uiState: SettingsUiState,
    defaultWorkTypes: List<String>,
    ctx: android.content.Context
) {
    val noProjectsMessage = stringResource(id = CoreR.string.no_projects_available)
    val generationSuccessMessage = stringResource(id = R.string.workday_generation_success)
    val generationErrorMessage = stringResource(id = R.string.workday_generation_error)

    LaunchedEffect(Unit) {
        settingsViewModel.loadSettings()
    }

    LaunchedEffect(uiState, defaultWorkTypes) {
        if (uiState is SettingsUiState.Success) {
            settingsViewModel.ensureDefaultWorkTypes(defaultWorkTypes)
        }
    }

    LaunchedEffect(
        settingsViewModel,
        ctx,
        noProjectsMessage,
        generationSuccessMessage,
        generationErrorMessage
    ) {
        settingsViewModel.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.TimesheetReportReady -> {
                    generateTimesheetReport(ctx = ctx, event = event)
                }
                is SettingsEvent.MonthlyReportError -> {
                    Toast.makeText(ctx, event.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.NoProjectsForMonth -> {
                    Toast.makeText(ctx, noProjectsMessage, Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.WorkdayGenerationSuccess -> {
                    val text = generationSuccessMessage.format(
                        event.insertedCount,
                        event.weekdayCandidates,
                        event.startDate,
                        event.endDate
                    )
                    Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.WorkdayGenerationError -> {
                    val text = generationErrorMessage.format(event.message)
                    Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

internal val INITIAL_FLEX_TIME_TOTAL_INPUT_REGEX = Regex(pattern = "[+-]?(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")

internal fun createSettingsActions(
    settingsViewModel: SettingsViewModel,
    successState: SettingsUiState.Success,
    generatedAllowanceLabels: GeneratedAllowanceLabels
): SettingsActions {
    return SettingsActions(
        onNameChange = {
            settingsViewModel.updateSettingsData { data -> data.copy(name = it) }
        },
        onEmployerChange = {
            settingsViewModel.updateSettingsData { data -> data.copy(employer = it) }
        },
        onDailyWorkTimeEstimateChange = {
            settingsViewModel.updateSettingsData { data -> data.copy(dailyWorkTimeEstimate = it) }
        },
        onDailyLunchTimeEstimateChange = {
            settingsViewModel.updateSettingsData { data -> data.copy(dailyLunchTimeEstimate = it) }
        },
        onInitialFlexTimeTotalChange = {
            settingsViewModel.updateSettingsData { data -> data.copy(initialFlexTimeTotal = it) }
        },
        onWorkTypeAdded = settingsViewModel::addWorkType,
        onWorkTypeRemoved = settingsViewModel::removeWorkType,
        onEnableTestFeaturesChange = {
            settingsViewModel.updateSettingsData { data -> data.copy(enableTestFeatures = it) }
        },
        onSave = { settingsViewModel.saveSettings() },
        onGenerateXlsx = {
            settingsViewModel.requestMonthlyReport(
                name = successState.data.name,
                employer = successState.data.employer
            )
        },
        onGenerateWorkdaysForMonth = {
            settingsViewModel.generateWorkdaysForSelected(
                scope = WorkdayGenerationScope.MONTH,
                allowanceLabels = generatedAllowanceLabels
            )
        },
        onGenerateWorkdaysForYear = {
            settingsViewModel.generateWorkdaysForSelected(
                scope = WorkdayGenerationScope.YEAR,
                allowanceLabels = generatedAllowanceLabels
            )
        }
    )
}

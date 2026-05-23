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
import com.akiwiksten.awtimesheet.feature.settings.R
import com.akiwiksten.awtimesheet.core.FORM_SECTION_SPACING
import com.akiwiksten.awtimesheet.core.ui.CenteredErrorBox
import com.akiwiksten.awtimesheet.core.ui.CenteredLoadingBox
import com.akiwiksten.awtimesheet.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.awtimesheet.feature.settings.components.SettingsContent
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val ctx = LocalContext.current
    val defaultWorkType = stringResource(id = R.string.other)
    val noProjectsMessage = stringResource(id = R.string.no_projects_available)

    LaunchedEffect(Unit) {
        settingsViewModel.loadSettings()
    }

    LaunchedEffect(uiState, defaultWorkType) {
        if (uiState is SettingsUiState.Success) {
            settingsViewModel.ensureDefaultWorkType(defaultWorkType)
        }
    }

    LaunchedEffect(settingsViewModel, ctx, noProjectsMessage) {
        settingsViewModel.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.TimesheetReportReady -> {
                    generateTimesheetReport(
                        ctx = ctx,
                        event = event
                    )
                }
                is SettingsEvent.MonthlyReportError -> {
                    Toast.makeText(ctx, event.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.NoProjectsForMonth -> {
                    Toast.makeText(ctx, noProjectsMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SettingsStateContent(
        uiState = uiState,
        defaultWorkType = defaultWorkType,
        createActions = { successState ->
            createSettingsActions(
                settingsViewModel = settingsViewModel,
                successState = successState
            )
        }
    )
}

private fun createSettingsActions(
    settingsViewModel: SettingsViewModel,
    successState: SettingsUiState.Success
): SettingsActions {
    return SettingsActions(
        onNameChange = settingsViewModel::setName,
        onEmployerChange = settingsViewModel::setEmployer,
        onDailyWorkTimeEstimateChange = settingsViewModel::setDailyWorkTimeEstimate,
        onDailyLunchTimeEstimateChange = settingsViewModel::setDailyLunchTimeEstimate,
        onInitialFlexTimeTotalChange = settingsViewModel::setInitialFlexTimeTotal,
        onWorkTypeAdded = settingsViewModel::addWorkType,
        onWorkTypeRemoved = settingsViewModel::removeWorkType,
        onSave = { settingsViewModel.saveSettings() },
        onGenerateXlsx = {
            settingsViewModel.requestMonthlyReport(
                name = successState.data.name,
                employer = successState.data.employer
            )
        }
    )
}

@Composable
internal fun SettingsStateContent(
    uiState: SettingsUiState,
    defaultWorkType: String,
    createActions: (SettingsUiState.Success) -> SettingsActions
) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = uiState is SettingsUiState.Loading
    )
    var lastSuccessState by remember { mutableStateOf<SettingsUiState.Success?>(value = null) }

    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.Success) {
            lastSuccessState = uiState
        }
    }

    when (uiState) {
        is SettingsUiState.Loading -> SettingsLoadingContent(
            showLoadingIndicator = showLoadingIndicator,
            lastSuccessState = lastSuccessState,
            defaultWorkType = defaultWorkType,
            createActions = createActions
        )
        is SettingsUiState.Success -> {
            val actions = remember(uiState) { createActions(uiState) }
            SettingsContent(
                uiState = uiState,
                actions = actions,
                defaultWorkType = defaultWorkType
            )
        }
        is SettingsUiState.Error -> CenteredErrorBox(
            errorMessage = stringResource(id = R.string.error_message, uiState.message),
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = FORM_SECTION_SPACING),
            fillMaxSize = false
        )
    }
}

@Composable
private fun SettingsLoadingContent(
    showLoadingIndicator: Boolean,
    lastSuccessState: SettingsUiState.Success?,
    defaultWorkType: String,
    createActions: (SettingsUiState.Success) -> SettingsActions
) {
    if (showLoadingIndicator) {
        CenteredLoadingBox(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = FORM_SECTION_SPACING),
            fillMaxSize = false
        )
    } else if (lastSuccessState != null) {
        val actions = remember(lastSuccessState) { createActions(lastSuccessState) }
        SettingsContent(
            uiState = lastSuccessState,
            actions = actions,
            defaultWorkType = defaultWorkType
        )
    } else {
        Box(modifier = Modifier.fillMaxSize())
    }
}

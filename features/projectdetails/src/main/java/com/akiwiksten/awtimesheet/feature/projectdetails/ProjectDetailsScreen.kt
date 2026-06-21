package com.akiwiksten.awtimesheet.feature.projectdetails

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.awtimesheet.core.TIME_FORMAT
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.hasChanges
import com.akiwiksten.awtimesheet.core.ui.AwtCenterAlignedTopAppBar
import com.akiwiksten.awtimesheet.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.isNewDayForProject
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsErrorState
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsLoadingState
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsSuccessState
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsUnsavedChangesDialog
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsDisplayParams
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsField
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsFieldActions
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsScreenActions
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsTimeFieldAction
import com.akiwiksten.awtimesheet.feature.projectdetails.model.updateTimeField
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    detailsArgs: ProjectDetailsState,
    onNavigateBack: () -> Unit,
    onConfirm: (ProjectDetailsState) -> Unit,
    viewModel: ProjectDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(detailsArgs) {
        viewModel.observeDateRepository(detailsArgs)
    }

    ProjectDetailsUiStateContent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onConfirm = onConfirm,
    ) { viewModel.deleteProjectDetails(it) }
}

@Composable
private fun ProjectDetailsUiStateContent(
    uiState: ProjectDetailsUiState,
    onNavigateBack: () -> Unit,
    onConfirm: (ProjectDetailsState) -> Unit,
    onDeleteDetails: (ProjectDetailsState) -> Unit,
) {
    when (uiState) {
        is ProjectDetailsUiState.Success -> {
            ProjectDetailsScreenStateful(
                uiState = uiState,
                onNavigateBack = onNavigateBack,
                onConfirm = onConfirm,
                onDeleteDetails = onDeleteDetails,
            )
        }
        else -> {
            ProjectDetailsScaffold(
                uiState = uiState,
                params = ProjectDetailsDisplayParams(
                    state = ProjectDetailsState(),
                    actions = ProjectDetailsScreenActions(),
                    isConfirmEnabled = false,
                    isAddMode = true
                ),
                onNavigateBack = onNavigateBack
            )
        }
    }
}

@Composable
private fun ProjectDetailsScreenStateful(
    uiState: ProjectDetailsUiState.Success,
    onNavigateBack: () -> Unit,
    onConfirm: (ProjectDetailsState) -> Unit,
    onDeleteDetails: (ProjectDetailsState) -> Unit
) {
    val initialDetails = uiState.details
    val settings = uiState.settings
    val persistedProjectTime = uiState.persistedProjectTime
    val unsavedMessage = stringResource(id = R.string.unsaved_data_message)

    // Keep in-progress edits through configuration changes, reset when baseline data changes.
    var state by rememberSaveable(initialDetails) {
        val hasProjectTimeOverride = (initialDetails.projectTime != persistedProjectTime) &&
            (initialDetails.projectTime != ZERO_TIME)

        val resolvedInitial = if (hasProjectTimeOverride) {
            initialDetails.updateTimeField(ProjectDetailsField.PROJECT_TIME, initialDetails.projectTime, settings)
        } else {
            initialDetails
        }
        mutableStateOf(value = resolvedInitial)
    }

    val hasUnsavedChanges by remember(state, initialDetails) {
        derivedStateOf { hasChanges(current = state, baseline = initialDetails) }
    }

    val actions = rememberProjectDetailsActions(
        state = state,
        settings = settings,
        onStateChange = { state = it },
        onConfirm = { onConfirm(state) },
    ) { onDeleteDetails(state) }

    val showUnsavedDialogState = rememberSaveable { mutableStateOf(value = false) }
    val handleBack: () -> Unit = {
        if (hasUnsavedChanges) {
            showUnsavedDialogState.value = true
        } else {
            onNavigateBack()
        }
    }

    BackHandler(onBack = handleBack)

    ProjectDetailsUnsavedChangesDialog(
        showState = showUnsavedDialogState,
        uiState = uiState,
        unsavedMessage = unsavedMessage,
        onNavigateBack = onNavigateBack,
        onConfirm = onConfirm
    )

    ProjectDetailsScaffold(
        uiState = uiState,
        params = ProjectDetailsDisplayParams(
            state = state,
            actions = actions,
            isConfirmEnabled = hasUnsavedChanges,
            isAddMode = initialDetails.isNewDayForProject()
        ),
        onNavigateBack = handleBack
    )
}

@Composable
private fun rememberProjectDetailsActions(
    state: ProjectDetailsState,
    settings: SettingsState,
    onStateChange: (ProjectDetailsState) -> Unit,
    onConfirm: () -> Unit,
    onDeleteDetails: () -> Unit
): ProjectDetailsScreenActions {
    val timeFormatter = remember { DateTimeFormatter.ofPattern(TIME_FORMAT) }
    return remember(state, settings) {
        ProjectDetailsScreenActions(
            onClearDetails = {
                onDeleteDetails()
                onStateChange(
                    state.copy(
                        startTime = ZERO_TIME,
                        endTime = ZERO_TIME,
                        lunchStart = ZERO_TIME,
                        lunchEnd = ZERO_TIME,
                        breakStart = ZERO_TIME,
                        breakEnd = ZERO_TIME,
                        projectTime = ZERO_TIME,
                        lunchTimeEstimate = settings.dailyLunchTimeEstimate
                    )
                )
            },
            onConfirm = onConfirm,
            fieldActions = createProjectDetailsFieldActions(
                state,
                settings,
                timeFormatter,
                onStateChange
            )
        )
    }
}

private fun createProjectDetailsFieldActions(
    state: ProjectDetailsState,
    settings: SettingsState,
    timeFormatter: DateTimeFormatter,
    onStateChange: (ProjectDetailsState) -> Unit
): ProjectDetailsFieldActions {
    fun createAction(field: ProjectDetailsField) = createTimeFieldAction(
        field,
        state,
        settings,
        timeFormatter,
        onStateChange
    )

    return ProjectDetailsFieldActions(
        startTime = createAction(ProjectDetailsField.START_TIME),
        lunchTime = createAction(ProjectDetailsField.LUNCH_TIME),
        endTime = createAction(ProjectDetailsField.END_TIME),
        projectTime = createAction(ProjectDetailsField.PROJECT_TIME),
        lunchStart = createAction(ProjectDetailsField.LUNCH_START),
        lunchEnd = createAction(ProjectDetailsField.LUNCH_END),
        breakStart = createAction(ProjectDetailsField.BREAK_START),
        breakEnd = createAction(ProjectDetailsField.BREAK_END)
    )
}

private fun createTimeFieldAction(
    field: ProjectDetailsField,
    state: ProjectDetailsState,
    settings: SettingsState,
    timeFormatter: DateTimeFormatter,
    onStateChange: (ProjectDetailsState) -> Unit
): ProjectDetailsTimeFieldAction {
    return ProjectDetailsTimeFieldAction(
        onCurrent = {
            onStateChange(state.updateTimeField(field, LocalTime.now().format(timeFormatter), settings))
        },
    ) {
        onStateChange(state.updateTimeField(field, it, settings))
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProjectDetailsScaffold(
    uiState: ProjectDetailsUiState,
    params: ProjectDetailsDisplayParams,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AwtCenterAlignedTopAppBar(
                title = stringResource(id = R.string.project_details),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { padding ->
        ProjectDetailsStateContent(
            padding = padding,
            uiState = uiState,
            params = params
        )
    }
}

@Composable
internal fun ProjectDetailsStateContent(
    padding: PaddingValues,
    uiState: ProjectDetailsUiState,
    params: ProjectDetailsDisplayParams
) {
    val contentPadding = PaddingValues(top = padding.calculateTopPadding())
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = uiState is ProjectDetailsUiState.Loading
    )
    var lastSuccessState by rememberSaveable { mutableStateOf<ProjectDetailsUiState.Success?>(value = null) }

    LaunchedEffect(uiState) {
        (uiState as? ProjectDetailsUiState.Success)?.let {
            lastSuccessState = it
        }
    }

    when (uiState) {
        is ProjectDetailsUiState.Loading -> {
            if (showLoadingIndicator) {
                ProjectDetailsLoadingState(padding = contentPadding)
            } else {
                lastSuccessState?.let { cachedState ->
                    ProjectDetailsSuccessState(
                        padding = contentPadding,
                        uiState = cachedState.copy(details = params.state),
                        actions = params.actions,
                        isConfirmEnabled = params.isConfirmEnabled,
                        isAddMode = params.isAddMode
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                )
            }
        }
        is ProjectDetailsUiState.Success -> ProjectDetailsSuccessState(
            padding = contentPadding,
            uiState = uiState.copy(details = params.state),
            actions = params.actions,
            isConfirmEnabled = params.isConfirmEnabled,
            isAddMode = params.isAddMode
        )
        is ProjectDetailsUiState.Error -> ProjectDetailsErrorState(
            padding = contentPadding,
            errorMessage = uiState.message
        )
    }
}

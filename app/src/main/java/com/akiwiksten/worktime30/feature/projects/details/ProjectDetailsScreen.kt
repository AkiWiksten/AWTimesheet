package com.akiwiksten.worktime30.feature.projects.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.hasChanges
import com.akiwiksten.worktime30.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.worktime30.core.ui.verticalScrollbar
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.isNewDayForProject
import com.akiwiksten.worktime30.feature.projects.details.components.ExistingDayFields
import com.akiwiksten.worktime30.feature.projects.details.components.FooterSection
import com.akiwiksten.worktime30.feature.projects.details.components.NewDayFields
import com.akiwiksten.worktime30.feature.projects.details.components.ProjectDetailsFieldActions
import com.akiwiksten.worktime30.feature.projects.details.components.TimeFieldAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    projectDetails: ProjectDetailsState,
    onNavigateBack: () -> Unit,
    onConfirm: (ProjectDetailsState, SettingsState) -> Unit,
    viewModel: ProjectDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val isInitialLoadComplete by viewModel.isInitialLoadComplete.collectAsState()
    val showUnsavedDialogState = remember { mutableStateOf(value = false) }
    val unsavedMessage = stringResource(id = R.string.unsaved_data_message)

    LaunchedEffect(projectDetails) {
        viewModel.observeDateRepository(projectDetails)
    }

    val baselineData = rememberBaselineData(
        uiState = uiState,
        isInitialLoadComplete = isInitialLoadComplete,
        projectDetails = projectDetails
    )

    val hasUnsavedChanges = baselineData != null &&
        (uiState as? ProjectDetailsUiState.Success)?.details?.let { current ->
            hasChanges(current = current, baseline = baselineData)
        } == true

    val shouldEnableConfirmForInitialProjectTime =
        (uiState as? ProjectDetailsUiState.Success)
            ?.details
            ?.projectTime
            ?.let { it.isNotBlank() && it != ZERO_TIME } == true

    val guardedNavigateBack = {
        if (hasUnsavedChanges) showUnsavedDialogState.value = true else onNavigateBack()
    }

    BackHandler(onBack = guardedNavigateBack)

    UnsavedChangesSection(
        showState = showUnsavedDialogState,
        uiState = uiState,
        unsavedMessage = unsavedMessage,
        onNavigateBack = onNavigateBack,
        onConfirm = onConfirm
    )

    Scaffold(
        topBar = { ProjectDetailsTopBar(onNavigateBack = guardedNavigateBack) }
    ) { padding ->
        val actions = remember(viewModel, onConfirm, uiState) {
            createProjectDetailsScreenActions(viewModel = viewModel) {
                val successState = uiState as? ProjectDetailsUiState.Success ?: return@createProjectDetailsScreenActions
                onConfirm(
                    successState.details,
                    successState.settings.copy(
                        dailyLunchTimeEstimate = successState.details.lunchTimeEstimate
                    )
                )
            }
        }

        ProjectDetailsStateContent(
            padding = padding,
            uiState = uiState,
            actions = actions,
            isConfirmEnabled = hasUnsavedChanges || shouldEnableConfirmForInitialProjectTime
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectDetailsTopBar(onNavigateBack: () -> Unit) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        CenterAlignedTopAppBar(
            title = {
                Header(
                    title = stringResource(id = R.string.project_details),
                    modifier = Modifier.padding(top = 0.dp),
                    fillMaxWidth = false
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        )
    }
}

@Composable
internal fun ProjectDetailsContent(
    padding: PaddingValues,
    uiState: ProjectDetailsUiState.Success,
    actions: ProjectDetailsScreenActions,
    isConfirmEnabled: Boolean
) {
    val scrollState = rememberScrollState()
    val helperTextResId = when {
        uiState.details.isNewDayForProject() -> R.string.add_new_project_details
        uiState.details.startTime != ZERO_TIME && uiState.details.projectTime == ZERO_TIME -> R.string.select_end_time
        else -> R.string.done_project
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = padding)
            .verticalScrollbar(scrollState = scrollState)
            .padding(16.dp, 16.dp, 16.dp, 0.dp)
            .verticalScroll(state = scrollState),
        verticalArrangement = Arrangement.spacedBy(space = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ProjectDetailsHeaderGroup(
            date = uiState.details.date,
            projectName = uiState.details.projectName,
            helperTextResId = helperTextResId,
            onClearDetails = actions.onClearDetails
        )

        if (uiState.details.isNewDayForProject()) {
            NewDayFields(uiState = uiState, actions = actions.fieldActions)
        } else {
            ExistingDayFields(uiState = uiState, actions = actions.fieldActions)
        }

        FooterSection(onConfirm = actions.onConfirm, isConfirmEnabled = isConfirmEnabled)
    }
}

@Composable
internal fun ProjectDetailsStateContent(
    padding: PaddingValues,
    uiState: ProjectDetailsUiState,
    actions: ProjectDetailsScreenActions,
    isConfirmEnabled: Boolean
) {
    val contentPadding = PaddingValues(top = padding.calculateTopPadding())
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = uiState is ProjectDetailsUiState.Loading
    )
    var lastSuccessState by remember { mutableStateOf<ProjectDetailsUiState.Success?>(value = null) }

    LaunchedEffect(uiState) {
        if (uiState is ProjectDetailsUiState.Success) {
            lastSuccessState = uiState
        }
    }

    when (uiState) {
        is ProjectDetailsUiState.Loading -> {
            if (showLoadingIndicator) {
                ProjectDetailsLoadingState(padding = contentPadding)
            } else {
                lastSuccessState?.let { cachedState ->
                    ProjectDetailsContent(
                        padding = contentPadding,
                        uiState = cachedState,
                        actions = actions,
                        isConfirmEnabled = isConfirmEnabled
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                )
            }
        }
        is ProjectDetailsUiState.Success -> ProjectDetailsContent(
            padding = contentPadding,
            uiState = uiState,
            actions = actions,
            isConfirmEnabled = isConfirmEnabled
        )
        is ProjectDetailsUiState.Error -> ProjectDetailsErrorState(
            padding = contentPadding,
            errorMessage = uiState.message
        )
    }
}

internal data class ProjectDetailsScreenActions(
    val onClearDetails: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val fieldActions: ProjectDetailsFieldActions = ProjectDetailsFieldActions()
)

private fun createProjectDetailsScreenActions(
    viewModel: ProjectDetailsViewModel,
    onConfirm: () -> Unit
): ProjectDetailsScreenActions {
    return ProjectDetailsScreenActions(
        onClearDetails = viewModel.clearDetails,
        onConfirm = onConfirm,
        fieldActions = ProjectDetailsFieldActions(
            startTime = TimeFieldAction(
                onCurrent = viewModel.currentStartTime,
                onSet = viewModel.setStartTime
            ),
            lunchTime = TimeFieldAction(
                onCurrent = viewModel.currentLunchTime,
                onSet = viewModel.setLunchTime
            ),
            endTime = TimeFieldAction(
                onCurrent = viewModel.currentEndTime,
                onSet = viewModel.setEndTime
            ),
            projectTime = TimeFieldAction(
                onCurrent = viewModel.currentProjectTime,
                onSet = viewModel.setProjectTime
            ),
            lunchStart = TimeFieldAction(
                onCurrent = viewModel.currentLunchStart,
                onSet = viewModel.setLunchStart
            ),
            lunchEnd = TimeFieldAction(
                onCurrent = viewModel.currentLunchEnd,
                onSet = viewModel.setLunchEnd
            ),
            breakStart = TimeFieldAction(
                onCurrent = viewModel.currentBreakStart,
                onSet = viewModel.setBreakStart
            ),
            breakEnd = TimeFieldAction(
                onCurrent = viewModel.currentBreakEnd,
                onSet = viewModel.setBreakEnd
            )
        )
    )
}


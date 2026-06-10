package com.akiwiksten.awtimesheet.feature.projectdetails

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.akiwiksten.awtimesheet.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsErrorState
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsLoadingState
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsSuccessState
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsTopBar
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsUnsavedChangesDialog
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsScreenActions
import com.akiwiksten.awtimesheet.feature.projectdetails.model.createProjectDetailsScreenActions

private data class ProjectDetailsRouteArgs(
    val projectName: String,
    val projectTime: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    projectName: String,
    projectTime: String,
    onNavigateBack: () -> Unit,
    onConfirm: (String, String) -> Unit,
    viewModel: ProjectDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestUiState by rememberUpdatedState(uiState)
    var skipDeleteDraftOnExit by rememberSaveable { mutableStateOf(false) }
    val showUnsavedDialogState = rememberSaveable { mutableStateOf(value = false) }
    val unsavedMessage = stringResource(id = R.string.unsaved_data_message)
    val routeArgs = remember(projectName, projectTime) {
        ProjectDetailsRouteArgs(projectName = projectName, projectTime = projectTime)
    }

    val navigateBackToSingleProject = {
        skipDeleteDraftOnExit = true
        onNavigateBack()
    }

    val confirmAndNavigateBackToSingleProject: (String, String) -> Unit = {
            confirmedProjectName,
            confirmedProjectTime,
        ->
        skipDeleteDraftOnExit = true
        onConfirm(confirmedProjectName, confirmedProjectTime)
    }

    ProjectDetailsLifecycleObserver(
        lifecycleOwner = lifecycleOwner,
        routeArgs = routeArgs,
        latestUiState = latestUiState,
        skipDeleteDraftOnExit = skipDeleteDraftOnExit,
        viewModel = viewModel
    )

    BackHandler(onBack = navigateBackToSingleProject)

    ProjectDetailsUnsavedChangesDialog(
        showState = showUnsavedDialogState,
        uiState = uiState,
        unsavedMessage = unsavedMessage,
        onNavigateBack = navigateBackToSingleProject,
        onConfirm = confirmAndNavigateBackToSingleProject
    )

    ProjectDetailsScaffold(
        uiState = uiState,
        viewModel = viewModel,
        onConfirmAndNavigateBack = confirmAndNavigateBackToSingleProject,
        onNavigateBack = onNavigateBack
    )
}

@Composable
private fun ProjectDetailsLifecycleObserver(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    routeArgs: ProjectDetailsRouteArgs,
    latestUiState: ProjectDetailsUiState,
    skipDeleteDraftOnExit: Boolean,
    viewModel: ProjectDetailsViewModel
) {
    DisposableEffect(lifecycleOwner, routeArgs, skipDeleteDraftOnExit, latestUiState) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    // Keep draft when intentionally returning to SingleProject.
                    if (!skipDeleteDraftOnExit) {
                        viewModel.deleteDraftProject(
                            (latestUiState as? ProjectDetailsUiState.Success)?.details?.projectName ?: ""
                        )
                    }
                }
                Lifecycle.Event.ON_START -> {
                    viewModel.observeDateRepository(routeArgs.projectName, routeArgs.projectTime)
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            // Composable left composition (route popped/replaced, etc.)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

@Composable
private fun ProjectDetailsScaffold(
    uiState: ProjectDetailsUiState,
    viewModel: ProjectDetailsViewModel,
    onConfirmAndNavigateBack: (String, String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = { ProjectDetailsTopBar(onNavigateBack = onNavigateBack) }
    ) { padding ->
        val actions = remember(viewModel, onConfirmAndNavigateBack, uiState) {
            createProjectDetailsScreenActions(viewModel = viewModel) {
                val successState = uiState as? ProjectDetailsUiState.Success
                    ?: return@createProjectDetailsScreenActions
                viewModel.saveProjectDetails(successState.details)
                onConfirmAndNavigateBack(
                    successState.details.projectName,
                    successState.details.projectTime
                )
            }
        }

        ProjectDetailsStateContent(
            padding = padding,
            uiState = uiState,
            actions = actions,
            isConfirmEnabled = true // shouldEnableConfirmForInitialProjectTime
        )
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
                    ProjectDetailsSuccessState(
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
        is ProjectDetailsUiState.Success -> ProjectDetailsSuccessState(
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

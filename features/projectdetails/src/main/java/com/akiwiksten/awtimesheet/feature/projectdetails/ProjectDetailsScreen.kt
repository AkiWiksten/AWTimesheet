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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsErrorState
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsLoadingState
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsSuccessState
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsTopBar
import com.akiwiksten.awtimesheet.feature.projectdetails.components.ProjectDetailsUnsavedChangesDialog
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsScreenActions
import com.akiwiksten.awtimesheet.feature.projectdetails.model.createProjectDetailsScreenActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    projectName: String,
    projectTime: String,
    onNavigateBack: () -> Unit,
    onConfirm: (String, String) -> Unit,
    viewModel: ProjectDetailsViewModel = hiltViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val isInitialLoadComplete by viewModel.isInitialLoadComplete.collectAsState()
    val showUnsavedDialogState = rememberSaveable { mutableStateOf(value = false) }
    val unsavedMessage = stringResource(id = R.string.unsaved_data_message)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    // Screen no longer visible (navigated away, app background, etc.)
                    viewModel.deleteDraftProject(
                        projectName = (uiState as? ProjectDetailsUiState.Success)?.details?.projectName ?: ""
                    )
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

    DisposableEffect(lifecycleOwner, projectName, projectTime) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.observeDateRepository(projectName = projectName, projectTime = projectTime)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val shouldEnableConfirmForInitialProjectTime = remember(uiState) {
        (uiState as? ProjectDetailsUiState.Success)
            ?.details
            ?.projectTime
            ?.let { it.isNotBlank() && it != ZERO_TIME } == true
    }

    BackHandler(onBack = onNavigateBack)

    ProjectDetailsUnsavedChangesDialog(
        showState = showUnsavedDialogState,
        uiState = uiState,
        unsavedMessage = unsavedMessage,
        onNavigateBack = onNavigateBack,
        onConfirm = onConfirm
    )

    Scaffold(
        topBar = { ProjectDetailsTopBar(onNavigateBack = onNavigateBack) }
    ) { padding ->
        val actions = remember(viewModel, onConfirm) {
            createProjectDetailsScreenActions(viewModel = viewModel) {
                val successState = uiState as? ProjectDetailsUiState.Success
                    ?: return@createProjectDetailsScreenActions
                viewModel.saveProjectDetails(successState.details)
                onConfirm(
                    successState.details.projectName,
                    successState.details.projectTime
                )
            }
        }

        ProjectDetailsStateContent(
            padding = padding,
            uiState = uiState,
            actions = actions,
            isConfirmEnabled = true// shouldEnableConfirmForInitialProjectTime
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

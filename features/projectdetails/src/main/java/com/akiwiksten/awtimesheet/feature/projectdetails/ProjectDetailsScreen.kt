package com.akiwiksten.awtimesheet.feature.projectdetails

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.isActionEnabled
import com.akiwiksten.awtimesheet.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
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
    detailsArgs: ProjectDetailsState,
    onNavigateBack: () -> Unit,
    onConfirm: (ProjectDetailsState) -> Unit,
    viewModel: ProjectDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasUnsavedChanges by viewModel.hasUnsavedChanges.collectAsState()
    val showUnsavedDialogState = rememberSaveable { mutableStateOf(value = false) }
    val unsavedMessage = stringResource(id = R.string.unsaved_data_message)

    LaunchedEffect(detailsArgs) {
        viewModel.observeDateRepository(detailsArgs)
    }

    val confirmAndNavigateBackToSingleProject: (ProjectDetailsState) -> Unit = {
            projectDetails,
        ->
        onConfirm(projectDetails)
    }

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
        onConfirm = confirmAndNavigateBackToSingleProject
    )

    ProjectDetailsScaffold(
        uiState = uiState,
        viewModel = viewModel,
        hasUnsavedChanges = hasUnsavedChanges,
        onConfirmAndNavigateBack = confirmAndNavigateBackToSingleProject,
        onNavigateBack = handleBack
    )
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

@Composable
private fun ProjectDetailsScaffold(
    uiState: ProjectDetailsUiState,
    viewModel: ProjectDetailsViewModel,
    hasUnsavedChanges: Boolean,
    onConfirmAndNavigateBack: (ProjectDetailsState) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = { ProjectDetailsTopBar(onNavigateBack = onNavigateBack) }
    ) { padding ->
        val actions = remember(viewModel, onConfirmAndNavigateBack, uiState) {
            createProjectDetailsScreenActions(viewModel = viewModel) {
                val successState = uiState as? ProjectDetailsUiState.Success
                    ?: return@createProjectDetailsScreenActions
                onConfirmAndNavigateBack(
                    successState.details,
                )
            }
        }

        val isConfirmEnabled = remember(uiState, hasUnsavedChanges) {
            val successState = uiState as? ProjectDetailsUiState.Success
            val details = successState?.details
            val hasRequiredFields = details != null &&
                details.projectName.isNotBlank() &&
                details.projectTime.isNotBlank() &&
                details.projectTime != ZERO_TIME

            isActionEnabled(
                hasRequiredFields = hasRequiredFields,
                hasUnsavedChanges = hasUnsavedChanges,
                allowWithoutChanges = true
            )
        }

        ProjectDetailsStateContent(
            padding = padding,
            uiState = uiState,
            actions = actions,
            isConfirmEnabled = isConfirmEnabled
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

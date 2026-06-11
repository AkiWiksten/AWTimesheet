package com.akiwiksten.awtimesheet.feature.singleproject

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.akiwiksten.awtimesheet.core.hasChanges
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectDerivedState
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectNavigationActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectRouteArgs
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectScreenState
import com.akiwiksten.awtimesheet.feature.singleproject.model.isDuplicateProjectName
import com.akiwiksten.awtimesheet.feature.singleproject.model.isSingleProjectConfirmEnabled
import com.akiwiksten.awtimesheet.feature.singleproject.model.resolveFullInitialSingleProjectState
import com.akiwiksten.awtimesheet.feature.singleproject.model.withAbsenceLogic
import com.akiwiksten.awtimesheet.feature.singleproject.model.withFlexDayLogic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleProjectScreen(
    routeArgs: SingleProjectRouteArgs,
    navigationActions: SingleProjectNavigationActions,
    viewModel: SingleProjectViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val savedText = stringResource(id = R.string.saved)
    val flexDayWorkType = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_flex_day)

    val openProjectDetails: (SingleProjectState) -> Unit = { state ->
        navigationActions.onOpenProjectDetails(state)
    }

    val saveAndNavigateBackToWorkday: (SingleProjectState) -> Unit = { state ->
        viewModel.saveProject(state, routeArgs.projectDetails)
        Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
        navigationActions.onNavigateBack()
    }

    LaunchedEffect(flexDayWorkType) {
        viewModel.setLocalizedFlexDayWorkType(flexDayWorkType)
    }

    LaunchedEffect(routeArgs) {
        viewModel.initializeState(
            projectName = routeArgs.projectName,
            projectTime = routeArgs.projectTime,
            isAddMode = routeArgs.isAddMode,
            listIndex = routeArgs.listIndex,
            kilometres = routeArgs.kilometres,
            allowance = routeArgs.allowance,
            workType = routeArgs.workType
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    SingleProjectUiStateContent(
        uiState = uiState,
        initialProjectNameArg = routeArgs.projectName,
        onNavigateBack = navigationActions.onNavigateBack,
        onOpenProjectDetails = openProjectDetails,
        onSaveAndNavigateBack = saveAndNavigateBackToWorkday
    )
}

@Composable
private fun SingleProjectLifecycleObserver(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    skipDeleteDraftOnExit: Boolean,
    onSkipDeleteDraftOnExitChanged: (Boolean) -> Unit,
    onDeleteDraft: () -> Unit
) {
    DisposableEffect(lifecycleOwner, skipDeleteDraftOnExit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // Reset when returning so future exits can clean up drafts normally.
                    onSkipDeleteDraftOnExitChanged(false)
                }
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    if (!skipDeleteDraftOnExit) {
                        // Screen no longer visible (navigated away, app background, etc.)
                        onDeleteDraft()
                    }
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
private fun SingleProjectUiStateContent(
    uiState: SingleProjectUiState,
    initialProjectNameArg: String,
    onNavigateBack: () -> Unit,
    onOpenProjectDetails: (SingleProjectState) -> Unit,
    onSaveAndNavigateBack: (SingleProjectState) -> Unit
) {
    when (uiState) {
        is SingleProjectUiState.Success -> {
            SingleProjectScreenStateful(
                uiState = uiState,
                initialProjectNameArg = initialProjectNameArg,
                onNavigateBack = onNavigateBack,
                onOpenProjectDetails = onOpenProjectDetails,
                onSaveAndNavigateBack = onSaveAndNavigateBack
            )
        }
        is SingleProjectUiState.Loading -> {
            // Show loading state or do nothing, don't try to render stateful yet
        }
        is SingleProjectUiState.Error -> {
            // Handle error state
        }
    }
}

@Composable
private fun SingleProjectScreenStateful(
    uiState: SingleProjectUiState,
    initialProjectNameArg: String,
    onNavigateBack: () -> Unit,
    onOpenProjectDetails: (SingleProjectState) -> Unit,
    onSaveAndNavigateBack: (SingleProjectState) -> Unit
) {
    val noAllowanceText = stringResource(id = R.string.no_allowance)
    val defaultWorkTypeText = stringResource(id = R.string.other)
    val absencePrefix = stringResource(id = R.string.absence_prefix)
    val flexDayWorkType = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_flex_day)

    val initialUiState = remember(
        uiState,
        noAllowanceText,
        defaultWorkTypeText,
        absencePrefix
    ) {
        resolveFullInitialSingleProjectState(
            uiState = uiState,
            noAllowanceText = noAllowanceText,
            defaultWorkTypeText = defaultWorkTypeText,
            absencePrefix = absencePrefix
        )
    }

    // Keep in-progress form edits through configuration changes, but reset when baseline data changes.
    var state by rememberSaveable(initialUiState) { mutableStateOf(value = initialUiState) }

    val derived = rememberSingleProjectDerivedState(
        state = state,
        initialUiState = initialUiState,
        singleProjectUiState = uiState,
    )

    // Build screen state from form state and derived flags
    val isAddMode = (uiState as? SingleProjectUiState.Success)?.data?.isAddMode ?: true
    val screenState = createSingleProjectScreenState(
        uiState = uiState,
        state = state,
        derived = derived,
        isProjectNameEditable = isAddMode || initialProjectNameArg.isBlank()
    )
    val actions = SingleProjectActions(
        onStateChange = { newState ->
            val settings = (uiState as? SingleProjectUiState.Success)?.settings
            state = newState
                .withAbsenceLogic(state, settings, absencePrefix)
                .withFlexDayLogic(
                    previousState = state,
                    noAllowanceText = noAllowanceText,
                    flexDayWorkType = flexDayWorkType
                )
        },
        onOpenProjectDetails = { onOpenProjectDetails(state) },
        onSave = { onSaveAndNavigateBack(state) }
    )

    val onDiscardAndNavigateBack = {
        state = initialUiState
        onNavigateBack()
    }

    SingleProjectScreenContent(
        screenState = screenState,
        actions = actions,
        hasUnsavedChanges = derived.hasUnsavedChanges,
        onNavigateBack = onNavigateBack,
        onDiscardAndNavigateBack = onDiscardAndNavigateBack
    )
}

private fun createSingleProjectScreenState(
    uiState: SingleProjectUiState,
    state: SingleProjectState,
    derived: SingleProjectDerivedState,
    isProjectNameEditable: Boolean
): SingleProjectScreenState {
    val successData = (uiState as? SingleProjectUiState.Success)?.data
    return SingleProjectScreenState(
        date = successData?.date ?: "",
        editedProjectIndex = successData?.listIndex ?: -1,
        state = state,
        isAddMode = successData?.isAddMode ?: true,
        isProjectNameEditable = isProjectNameEditable,
        uiState = uiState,
        isConfirmEnabled = derived.isConfirmEnabled,
        isDuplicateProjectName = derived.isDuplicate
    )
}

@Composable
private fun rememberSingleProjectDerivedState(
    state: SingleProjectState,
    initialUiState: SingleProjectState,
    singleProjectUiState: SingleProjectUiState
): SingleProjectDerivedState {
    val hasUnsavedChanges by remember(state, initialUiState) {
        derivedStateOf { hasChanges(current = state, baseline = initialUiState) }
    }
    val isDuplicate by remember(state.projectName, singleProjectUiState, initialUiState.listIndex) {
        derivedStateOf {
            isDuplicateProjectName(
                projectName = state.projectName,
                currentIndex = initialUiState.listIndex,
                singleProjectState = (singleProjectUiState as? SingleProjectUiState.Success)?.data
            )
        }
    }
    val isConfirmEnabled by remember(state, hasUnsavedChanges, isDuplicate, initialUiState.listIndex) {
        derivedStateOf {
            isSingleProjectConfirmEnabled(
                state = state,
                hasUnsavedChanges = hasUnsavedChanges,
                isDuplicateProjectName = isDuplicate,
                isAddMode = initialUiState.isAddMode
            )
        }
    }
    return SingleProjectDerivedState(
        hasUnsavedChanges = hasUnsavedChanges,
        isDuplicate = isDuplicate,
        isConfirmEnabled = isConfirmEnabled
    )
}

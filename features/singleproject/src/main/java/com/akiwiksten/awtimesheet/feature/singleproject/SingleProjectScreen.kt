package com.akiwiksten.awtimesheet.feature.singleproject

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
import com.akiwiksten.awtimesheet.core.hasChanges
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectConfiguration
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectDerivedState
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectNavigationActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectRouteArgs
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectScreenParams
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectScreenState
import com.akiwiksten.awtimesheet.feature.singleproject.model.isDuplicateProjectName
import com.akiwiksten.awtimesheet.feature.singleproject.model.isSingleProjectConfirmEnabled
import com.akiwiksten.awtimesheet.feature.singleproject.model.resolveFullInitialSingleProjectState
import com.akiwiksten.awtimesheet.feature.singleproject.model.withAbsenceLogic

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

    val openProjectDetails: (SingleProjectState, ProjectDetailsState?) -> Unit = { state, currentDetails ->
        val details = (
            currentDetails ?: ProjectDetailsState(
                date = state.date,
                projectName = state.projectName
            )
            ).copy(projectTime = state.projectTime)
        navigationActions.onOpenProjectDetails(state, details)
    }

    val saveAndNavigateBackToWorkday: (SingleProjectState, ProjectDetailsState?) -> Unit = { state, details ->
        viewModel.saveProject(state, details)
        Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
        navigationActions.onNavigateBack()
    }

    LaunchedEffect(flexDayWorkType) {
        viewModel.setLocalizedFlexDayWorkType(flexDayWorkType)
    }

    LaunchedEffect(routeArgs) {
        viewModel.initializeState(
            routeArgs
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
private fun SingleProjectUiStateContent(
    uiState: SingleProjectUiState,
    initialProjectNameArg: String,
    onNavigateBack: () -> Unit,
    onOpenProjectDetails: (SingleProjectState, ProjectDetailsState?) -> Unit,
    onSaveAndNavigateBack: (SingleProjectState, ProjectDetailsState?) -> Unit
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
    uiState: SingleProjectUiState.Success,
    initialProjectNameArg: String,
    onNavigateBack: () -> Unit,
    onOpenProjectDetails: (SingleProjectState, ProjectDetailsState?) -> Unit,
    onSaveAndNavigateBack: (SingleProjectState, ProjectDetailsState?) -> Unit
) {
    val noAllowanceText = stringResource(id = R.string.no_allowance)
    val defaultWorkTypeText = stringResource(id = R.string.other)
    val absencePrefix = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.absence_prefix)
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
    val isAddMode = uiState.data.isAddMode
    val screenState = createSingleProjectScreenState(
        uiState = uiState,
        state = state,
        derived = derived,
        isProjectNameEditable = isAddMode || initialProjectNameArg.isBlank()
    )
    val actions = SingleProjectActions(
        onStateChange = { newState ->
            val settings = uiState.settings
            state = newState
                .withAbsenceLogic(state, settings, absencePrefix, flexDayWorkType)
        },
        onOpenProjectDetails = { onOpenProjectDetails(state, uiState.projectDetails) },
        onSave = { onSaveAndNavigateBack(state, uiState.projectDetails) }
    )

    val onDiscardAndNavigateBack = {
        state = initialUiState
        onNavigateBack()
    }

    SingleProjectScreenContent(
        params = SingleProjectScreenParams(
            screenState = screenState,
            actions = actions,
            config = SingleProjectConfiguration(
                absencePrefix = absencePrefix,
                flexDayWorkType = flexDayWorkType
            )
        ),
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
        isDuplicateProjectName = derived.isDuplicate,
        isTimePickerDisabled = (uiState as? SingleProjectUiState.Success)?.projectDetails != null
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
    val isDuplicate by remember(state.projectName, singleProjectUiState) {
        derivedStateOf {
            isDuplicateProjectName(
                projectName = state.projectName,
                otherProjectNames = (singleProjectUiState as? SingleProjectUiState.Success)
                    ?.otherProjectNames ?: emptyList()
            )
        }
    }
    val isConfirmEnabled by remember(
        state,
        hasUnsavedChanges,
        isDuplicate,
        initialUiState.listIndex,
        singleProjectUiState
    ) {
        derivedStateOf {
            isSingleProjectConfirmEnabled(
                state = state,
                hasUnsavedChanges = hasUnsavedChanges,
                isDuplicateProjectName = isDuplicate,
                isAddMode = initialUiState.isAddMode,
                hasProjectDetails = (singleProjectUiState as? SingleProjectUiState.Success)?.projectDetails != null
            )
        }
    }
    return SingleProjectDerivedState(
        hasUnsavedChanges = hasUnsavedChanges,
        isDuplicate = isDuplicate,
        isConfirmEnabled = isConfirmEnabled
    )
}

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
import com.akiwiksten.awtimesheet.core.R as CoreR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleProjectScreen(
    routeArgs: SingleProjectRouteArgs,
    navigationActions: SingleProjectNavigationActions,
    viewModel: SingleProjectViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val savedText = stringResource(id = CoreR.string.saved)
    val flexDayWorkType = stringResource(id = CoreR.string.work_type_flex_day)

    val openProjectDetails: (SingleProjectState, ProjectDetailsState?) -> Unit = { state, currentDetails ->
        val base = currentDetails ?: ProjectDetailsState(
            date = state.date,
            projectName = state.projectName,
            originalProjectName = routeArgs.originalProjectName
        )
        val details = base.copy(
            projectTime = state.projectTime,
            projectName = state.projectName,
            originalProjectName = routeArgs.originalProjectName
        )
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

    val contentParams = SingleProjectContentParams(
        initialProjectNameArg = routeArgs.projectName,
        onNavigateBack = navigationActions.onNavigateBack,
        onOpenProjectDetails = openProjectDetails,
        onSaveAndNavigateBack = saveAndNavigateBackToWorkday,
        onNavigateToLocationPicker = { navigationActions.onNavigateToLocationPicker(it) }
    )

    SingleProjectUiStateContent(
        uiState = uiState,
        params = contentParams,
    )
}

private data class SingleProjectContentParams(
    val initialProjectNameArg: String,
    val onNavigateBack: () -> Unit,
    val onOpenProjectDetails: (SingleProjectState, ProjectDetailsState?) -> Unit,
    val onSaveAndNavigateBack: (SingleProjectState, ProjectDetailsState?) -> Unit,
    val onNavigateToLocationPicker: (SingleProjectState) -> Unit,
)

@Composable
private fun SingleProjectUiStateContent(
    uiState: SingleProjectUiState,
    params: SingleProjectContentParams,
) {
    when (uiState) {
        is SingleProjectUiState.Success -> {
            SingleProjectScreenStateful(
                uiState = uiState,
                params = params,
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
    params: SingleProjectContentParams,
) {
    val noAllowanceText = stringResource(id = CoreR.string.no_allowance)
    val defaultWorkTypeText = stringResource(id = CoreR.string.other)
    val absencePrefix = stringResource(id = CoreR.string.absence_prefix)
    val flexDayWorkType = stringResource(id = CoreR.string.work_type_flex_day)

    val baseline = rememberSingleProjectBaseline(uiState, noAllowanceText, defaultWorkTypeText, absencePrefix)
    val initialState = rememberSingleProjectInitialState(uiState, noAllowanceText, defaultWorkTypeText, absencePrefix)

    // Keep in-progress form edits through configuration changes, but reset when baseline data changes.
    var state by rememberSaveable(initialState) { mutableStateOf(value = initialState) }

    val derived = rememberSingleProjectDerivedState(
        state = state,
        baseline = baseline,
        singleProjectUiState = uiState,
    )

    // Build screen state from form state and derived flags
    val screenState = createSingleProjectScreenState(
        uiState = uiState,
        state = state,
        derived = derived,
        isProjectNameEditable = true
    )
    val actions = SingleProjectActions(
        onStateChange = { newState ->
            val settings = uiState.settings
            state = newState
                .withAbsenceLogic(state, settings, absencePrefix, flexDayWorkType)
        },
        onOpenProjectDetails = { params.onOpenProjectDetails(state, uiState.projectDetails) },
        onSave = { params.onSaveAndNavigateBack(state, uiState.projectDetails) },
        onNavigateToLocationPicker = { params.onNavigateToLocationPicker(state) }
    )

    val onDiscardAndNavigateBack = {
        state = baseline
        params.onNavigateBack()
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
        onNavigateBack = params.onNavigateBack,
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
    baseline: SingleProjectState,
    singleProjectUiState: SingleProjectUiState
): SingleProjectDerivedState {
    val hasUnsavedChanges by remember(state, baseline) {
        derivedStateOf { hasChanges(current = state, baseline = baseline) }
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
        baseline.listIndex,
        singleProjectUiState
    ) {
        derivedStateOf {
            isSingleProjectConfirmEnabled(
                state = state,
                hasUnsavedChanges = hasUnsavedChanges,
                isDuplicateProjectName = isDuplicate,
                isAddMode = baseline.isAddMode,
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

@Composable
private fun rememberSingleProjectBaseline(
    uiState: SingleProjectUiState.Success,
    noAllowanceText: String,
    defaultWorkTypeText: String,
    absencePrefix: String
) = remember(
    uiState.baseline,
    noAllowanceText,
    defaultWorkTypeText,
    absencePrefix
) {
    resolveFullInitialSingleProjectState(
        data = uiState.baseline,
        uiState = uiState,
        noAllowanceText = noAllowanceText,
        defaultWorkTypeText = defaultWorkTypeText,
        absencePrefix = absencePrefix
    )
}

@Composable
private fun rememberSingleProjectInitialState(
    uiState: SingleProjectUiState.Success,
    noAllowanceText: String,
    defaultWorkTypeText: String,
    absencePrefix: String
) = remember(
    uiState.data,
    noAllowanceText,
    defaultWorkTypeText,
    absencePrefix
) {
    resolveFullInitialSingleProjectState(
        data = uiState.data,
        uiState = uiState,
        noAllowanceText = noAllowanceText,
        defaultWorkTypeText = defaultWorkTypeText,
        absencePrefix = absencePrefix
    )
}

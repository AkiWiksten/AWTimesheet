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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.awtimesheet.core.hasChanges
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectDerivedState
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectNavigationActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectScreenArgs
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectScreenState
import com.akiwiksten.awtimesheet.feature.singleproject.model.isDuplicateProjectName
import com.akiwiksten.awtimesheet.feature.singleproject.model.isSingleProjectConfirmEnabled
import com.akiwiksten.awtimesheet.feature.singleproject.model.resolveFullInitialSingleProjectState
import com.akiwiksten.awtimesheet.feature.singleproject.model.withAbsenceLogic
import com.akiwiksten.awtimesheet.feature.singleproject.model.withFlexDayLogic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleProjectScreen(
    args: SingleProjectScreenArgs,
    navigationActions: SingleProjectNavigationActions,
    viewModel: SingleProjectViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val savedText = stringResource(id = R.string.saved)
    val flexDayWorkType = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_flex_day)

    LaunchedEffect(flexDayWorkType) {
        viewModel.setLocalizedFlexDayWorkType(flexDayWorkType)
    }

    LaunchedEffect(
        args.initialSingleProjectState.date,
        args.initialSingleProjectState.projectName,
        args.initialSingleProjectState.projectTime
    ) {
        viewModel.initializeState(
            singleProjectState = args.initialSingleProjectState,
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    SingleProjectScreenStateful(
        args = args,
        uiState = uiState,
        onNavigateBack = navigationActions.onNavigateBack,
        onOpenProjectDetails = navigationActions.onOpenProjectDetails,
        onSave = { state ->
            viewModel.saveProject(state, args.initialProjectDetails, args.initialSettings)
            Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
        }
    )
}

@Composable
private fun SingleProjectScreenStateful(
    args: SingleProjectScreenArgs,
    uiState: SingleProjectUiState,
    onNavigateBack: () -> Unit,
    onOpenProjectDetails: (SingleProjectState, ProjectDetailsState?) -> Unit,
    onSave: (SingleProjectState) -> Unit
) {
    val noAllowanceText = stringResource(id = R.string.no_allowance)
    val defaultWorkTypeText = stringResource(id = R.string.other)
    val absencePrefix = stringResource(id = R.string.absence_prefix)
    val flexDayWorkType = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_flex_day)

    val initialUiState = remember(
        args,
        uiState,
        noAllowanceText,
        defaultWorkTypeText,
        absencePrefix
    ) {
        resolveFullInitialSingleProjectState(
            args = args,
            uiState = uiState,
            noAllowanceText = noAllowanceText,
            defaultWorkTypeText = defaultWorkTypeText,
            absencePrefix = absencePrefix
        )
    }

    var state by remember(initialUiState) { mutableStateOf(value = initialUiState) }

    // Mutable state for form edits; updates when ViewModel data changes (e.g., returning from ProjectDetailsScreen)
    LaunchedEffect(initialUiState) { state = initialUiState }

    val derived = rememberSingleProjectDerivedState(
        state = state,
        initialUiState = initialUiState,
        singleProjectUiState = uiState,
        currentIndex = args.initialSingleProjectState.index
    )

    // Build screen state from form state and derived flags
    val screenState = createSingleProjectScreenState(
        args = args,
        uiState = uiState,
        state = state,
        derived = derived
    )
    val actions = SingleProjectActions(
        onStateChange = { newState ->
            val settings = (uiState as? SingleProjectUiState.Success)?.settings ?: args.initialSettings
            state = newState
                .withAbsenceLogic(state, settings, absencePrefix)
                .withFlexDayLogic(
                    previousState = state,
                    noAllowanceText = noAllowanceText,
                    flexDayWorkType = flexDayWorkType
                )
        },
        onOpenProjectDetails = { onOpenProjectDetails(state, args.initialProjectDetails) },
        onConfirm = {
            onSave(state)
            onNavigateBack()
        }
    )

    SingleProjectScreenContent(
        screenState = screenState,
        actions = actions,
        hasUnsavedChanges = derived.hasUnsavedChanges,
        onNavigateBack = onNavigateBack
    )
}

private fun createSingleProjectScreenState(
    args: SingleProjectScreenArgs,
    uiState: SingleProjectUiState,
    state: SingleProjectState,
    derived: SingleProjectDerivedState
): SingleProjectScreenState {
    val successData = (uiState as? SingleProjectUiState.Success)?.data
    return SingleProjectScreenState(
        date = successData?.date ?: "",
        editedProjectIndex = args.initialSingleProjectState.index,
        state = state,
        isAddMode = args.initialSingleProjectState.index == -1,
        uiState = uiState,
        isConfirmEnabled = derived.isConfirmEnabled,
        isDuplicateProjectName = derived.isDuplicate
    )
}

@Composable
private fun rememberSingleProjectDerivedState(
    state: SingleProjectState,
    initialUiState: SingleProjectState,
    currentIndex: Int,
    singleProjectUiState: SingleProjectUiState
): SingleProjectDerivedState {
    val hasUnsavedChanges by remember(state, initialUiState) {
        derivedStateOf { hasChanges(current = state, baseline = initialUiState) }
    }
    val isDuplicate by remember(state.projectName, singleProjectUiState, currentIndex) {
        derivedStateOf {
            isDuplicateProjectName(
                projectName = state.projectName,
                currentIndex = currentIndex,
                singleProjectState = (singleProjectUiState as? SingleProjectUiState.Success)?.data
            )
        }
    }
    val isConfirmEnabled by remember(state, hasUnsavedChanges, isDuplicate, currentIndex) {
        derivedStateOf {
            isSingleProjectConfirmEnabled(
                state = state,
                hasUnsavedChanges = hasUnsavedChanges,
                isDuplicateProjectName = isDuplicate,
                isAddMode = currentIndex == -1
            )
        }
    }
    return SingleProjectDerivedState(
        hasUnsavedChanges = hasUnsavedChanges,
        isDuplicate = isDuplicate,
        isConfirmEnabled = isConfirmEnabled
    )
}

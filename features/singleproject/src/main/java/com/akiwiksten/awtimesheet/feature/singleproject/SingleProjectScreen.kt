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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    projectName: String,
    projectTime: String,
    isAddMode: Boolean,
    listIndex: Int,
    navigationActions: SingleProjectNavigationActions,
    viewModel: SingleProjectViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val savedText = stringResource(id = R.string.saved)
    val flexDayWorkType = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_flex_day)

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY -> {
                    // Screen no longer visible (navigated away, app background, etc.)
                    viewModel.deleteDraftProject()
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

    LaunchedEffect(flexDayWorkType) {
        viewModel.setLocalizedFlexDayWorkType(flexDayWorkType)
    }

    LaunchedEffect(projectName, projectTime, isAddMode, listIndex) {
        viewModel.initializeState(
            projectName = projectName,
            projectTime = projectTime,
            isAddMode = isAddMode,
            listIndex = listIndex
        )
    }

    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is SingleProjectUiState.Success -> {
            SingleProjectScreenStateful(
                uiState = uiState,
                onNavigateBack = navigationActions.onNavigateBack,
                onOpenProjectDetails = { state ->
                    viewModel.saveProject(state = state, isDraft = true)
                    navigationActions.onOpenProjectDetails(state)
                },
                onSave = { state ->
                    viewModel.saveProject(state)
                    Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
                }
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
    onNavigateBack: () -> Unit,
    onOpenProjectDetails: (SingleProjectState) -> Unit,
    onSave: (SingleProjectState) -> Unit
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

    var state by remember(initialUiState) { mutableStateOf(value = initialUiState) }

    // Mutable state for form edits; updates when ViewModel data changes (e.g., returning from ProjectDetailsScreen)
    LaunchedEffect(initialUiState) { state = initialUiState }

    val derived = rememberSingleProjectDerivedState(
        state = state,
        initialUiState = initialUiState,
        singleProjectUiState = uiState,
    )

    // Build screen state from form state and derived flags
    val screenState = createSingleProjectScreenState(
        uiState = uiState,
        state = state,
        derived = derived
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
    uiState: SingleProjectUiState,
    state: SingleProjectState,
    derived: SingleProjectDerivedState
): SingleProjectScreenState {
    val successData = (uiState as? SingleProjectUiState.Success)?.data
    return SingleProjectScreenState(
        date = successData?.date ?: "",
        editedProjectIndex = successData?.listIndex ?: -1,
        state = state,
        isAddMode = successData?.isAddMode ?: true,
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

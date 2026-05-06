package com.akiwiksten.worktime30.feature.projects.single

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.FIELD_CORNER_RADIUS
import com.akiwiksten.worktime30.core.FORM_SECTION_SPACING
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.core.ui.sharedActivityViewModel
import com.akiwiksten.worktime30.core.ui.UnsavedChangesDialog
import com.akiwiksten.worktime30.core.ui.hasChanges
import com.akiwiksten.worktime30.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.worktime30.core.ui.verticalScrollbar
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.feature.projects.single.components.DialogDropdownFields
import com.akiwiksten.worktime30.feature.workday.WorkdayUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleProjectScreen(
    args: SingleProjectScreenArgs,
    navigationActions: SingleProjectNavigationActions,
    projectsUiState: WorkdayUiState,
    onSaved: () -> Unit = {},
    singleProjectViewModel: SingleProjectViewModel = sharedActivityViewModel()
) {
    val context = LocalContext.current
    val savedText = stringResource(id = R.string.saved)
    val noAllowanceText = stringResource(id = R.string.no_allowance)
    val defaultWorkTypeText = stringResource(id = R.string.other)

    SingleProjectScreenStateful(
        args = args,
        config = SingleProjectScreenStatefulConfig(
            projectsUiState = projectsUiState,
            noAllowanceText = noAllowanceText,
            defaultWorkTypeText = defaultWorkTypeText,
            onNavigateBack = navigationActions.onNavigateBack,
            onOpenProjectDetails = navigationActions.onOpenProjectDetails,
            onSave = { state ->
                singleProjectViewModel.saveProject(state, args.initialProjectDetails, args.initialSettings)
                onSaved()
                Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
            }
        )
    )
}

@Composable
private fun SingleProjectScreenStateful(
    args: SingleProjectScreenArgs,
    config: SingleProjectScreenStatefulConfig
) {
    val projectsUiState = config.projectsUiState
    val date = (projectsUiState as? WorkdayUiState.Success)?.date ?: ""

    val initialUiState = remember(
        args.initialSingleProjectState.index,
        projectsUiState,
        args.initialSingleProjectState.projectTime,
        args.initialSingleProjectState.projectName,
        args.initialProjectDetails,
        args.initialSettings,
        config.noAllowanceText,
        config.defaultWorkTypeText
    ) {
        resolveInitialSingleProjectState(
            initialSingleProjectState = args.initialSingleProjectState,
            initialProjectDetails = args.initialProjectDetails,
            initialSettings = args.initialSettings,
            projectsUiState = projectsUiState
        )
            .withDefaultAllowance(defaultAllowance = config.noAllowanceText)
            .withDefaultWorkType(defaultWorkType = config.defaultWorkTypeText)
    }

    var state by remember(initialUiState) { mutableStateOf(value = initialUiState) }

    // Update state when the ViewModel data changes (e.g., returning from ProjectDetailsScreen)
    LaunchedEffect(initialUiState) { state = initialUiState }

    val derived = rememberSingleProjectDerivedState(
        state = state,
        initialUiState = initialUiState,
        projectsUiState = projectsUiState,
        currentIndex = args.initialSingleProjectState.index
    )

    SingleProjectScreenContent(
        params = SingleProjectScreenContentParams(
            index = args.initialSingleProjectState.index,
            date = date,
            state = state,
            isAddMode = args.initialSingleProjectState.index == -1,
            projectsUiState = projectsUiState,
            isConfirmEnabled = derived.isConfirmEnabled,
            hasUnsavedChanges = derived.hasUnsavedChanges,
            isDuplicateProjectName = derived.isDuplicate,
            onStateChange = { state = it },
            onNavigateBack = config.onNavigateBack,
            onOpenProjectDetails = {
                config.onOpenProjectDetails(state, args.initialProjectDetails, args.initialSettings)
            },
            onConfirm = {
                config.onSave(state)
                config.onNavigateBack()
            }
        )
    )
}

private data class SingleProjectScreenStatefulConfig(
    val projectsUiState: WorkdayUiState,
    val noAllowanceText: String,
    val defaultWorkTypeText: String,
    val onNavigateBack: () -> Unit,
    val onOpenProjectDetails: (SingleProjectState, ProjectDetailsState?, SettingsState?) -> Unit,
    val onSave: (SingleProjectState) -> Unit
)

data class SingleProjectScreenArgs(
    val initialSingleProjectState: SingleProjectState,
    val initialProjectDetails: ProjectDetailsState? = null,
    val initialSettings: SettingsState? = null
)

data class SingleProjectNavigationActions(
    val onNavigateBack: () -> Unit,
    val onOpenProjectDetails: (SingleProjectState, ProjectDetailsState?, SettingsState?) -> Unit
)

internal data class SingleProjectDerivedState(
    val hasUnsavedChanges: Boolean,
    val isDuplicate: Boolean,
    val isConfirmEnabled: Boolean
)

@Composable
private fun rememberSingleProjectDerivedState(
    state: SingleProjectState,
    initialUiState: SingleProjectState,
    projectsUiState: WorkdayUiState,
    currentIndex: Int
): SingleProjectDerivedState {
    val hasUnsavedChanges by remember(state, initialUiState) {
        derivedStateOf { hasChanges(current = state, baseline = initialUiState) }
    }
    val isDuplicate by remember(state.projectName, projectsUiState, currentIndex) {
        derivedStateOf {
            isDuplicateProjectName(
                projectName = state.projectName,
                currentIndex = currentIndex,
                projects = (projectsUiState as? WorkdayUiState.Success)?.projects ?: emptyList()
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

@Composable
internal fun SingleProjectScreenContent(params: SingleProjectScreenContentParams) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = params.projectsUiState is WorkdayUiState.Loading
    )
    val lastSuccessState = remember { mutableStateOf<WorkdayUiState.Success?>(value = null) }
    val showUnsavedDialogState = remember { mutableStateOf(value = false) }
    val unsavedMessage = stringResource(id = R.string.unsaved_data_message)

    LaunchedEffect(params.projectsUiState) {
        if (params.projectsUiState is WorkdayUiState.Success) {
            lastSuccessState.value = params.projectsUiState
        }
    }

    val guardedNavigateBack = {
        if (params.hasUnsavedChanges) {
            showUnsavedDialogState.value = true
        } else {
            params.onNavigateBack()
        }
    }

    if (showUnsavedDialogState.value) {
        UnsavedChangesDialog(
            onDismiss = { showUnsavedDialogState.value = false },
            onDiscard = params.onNavigateBack,
            onSave = params.onConfirm,
            dialogText = unsavedMessage
        )
    }

    val actions = SingleProjectActions(
        onStateChange = params.onStateChange,
        onOpenProjectDetails = params.onOpenProjectDetails,
        onConfirm = params.onConfirm
    )

    Scaffold(
        topBar = {
            SingleProjectTopSection(onNavigateBack = guardedNavigateBack)
        }
    ) { padding ->
        SingleProjectContentByUiState(
            padding = padding,
            params = params,
            actions = actions,
            showLoadingIndicator = showLoadingIndicator,
            cachedSuccessState = lastSuccessState.value
        )
    }
}

@Composable
private fun SingleProjectContentByUiState(
    padding: PaddingValues,
    params: SingleProjectScreenContentParams,
    actions: SingleProjectActions,
    showLoadingIndicator: Boolean,
    cachedSuccessState: WorkdayUiState.Success?
) {
    when (params.projectsUiState) {
        is WorkdayUiState.Loading -> SingleProjectLoadingContent(
            padding = padding,
            params = params,
            actions = actions,
            showLoadingIndicator = showLoadingIndicator,
            cachedSuccessState = cachedSuccessState
        )

        is WorkdayUiState.Success -> SingleProjectSuccessContent(
            padding = padding,
            params = params,
            actions = actions,
            uiState = params.projectsUiState
        )

        is WorkdayUiState.Error -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Error: ${params.projectsUiState.message}",
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun SingleProjectLoadingContent(
    padding: PaddingValues,
    params: SingleProjectScreenContentParams,
    actions: SingleProjectActions,
    showLoadingIndicator: Boolean,
    cachedSuccessState: WorkdayUiState.Success?
) {
    if (showLoadingIndicator) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    SingleProjectSuccessContent(
        padding = padding,
        params = params,
        actions = actions,
        uiState = cachedSuccessState ?: WorkdayUiState.Success(date = params.date)
    )
}

@Composable
private fun SingleProjectSuccessContent(
    padding: PaddingValues,
    params: SingleProjectScreenContentParams,
    actions: SingleProjectActions,
    uiState: WorkdayUiState
) {
    val successState = uiState as? WorkdayUiState.Success
    val originalProjectTime = successState
        ?.projects
        ?.find { it.index == params.index }
        ?.projectTime
        ?: ZERO_TIME
    val baseWithoutCurrent = WorkTimeCalculator.calculateFlexTime(
        initialTime = successState?.workTimeByDate ?: ZERO_TIME,
        addedTime = "-$originalProjectTime"
    )
    val workTimeByDate = WorkTimeCalculator.calculateFlexTime(
        initialTime = baseWithoutCurrent,
        addedTime = params.state.projectTime
    )

    SingleProjectContent(
        padding = padding,
        workTimeByDate = workTimeByDate,
        screenState = SingleProjectScreenState(
            date = params.date,
            editedProjectIndex = params.index,
            state = params.state,
            isAddMode = params.isAddMode,
            uiState = uiState,
            isConfirmEnabled = params.isConfirmEnabled,
            isDuplicateProjectName = params.isDuplicateProjectName
        ),
        actions = actions
    )
}

data class SingleProjectScreenState(
    val date: String,
    val editedProjectIndex: Int,
    val state: SingleProjectState,
    val isAddMode: Boolean,
    val uiState: WorkdayUiState,
    val isConfirmEnabled: Boolean,
    val isDuplicateProjectName: Boolean
)

data class SingleProjectActions(
    val onStateChange: (SingleProjectState) -> Unit,
    val onOpenProjectDetails: () -> Unit,
    val onConfirm: () -> Unit
)

@Composable
private fun SingleProjectTopSection(
    onNavigateBack: () -> Unit
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            SingleProjectTopBar(onNavigateBack = onNavigateBack)
        }
    }
}

@Composable
private fun SingleProjectContent(
    padding: PaddingValues,
    workTimeByDate: String,
    screenState: SingleProjectScreenState,
    actions: SingleProjectActions
) {
    val scrollState = rememberScrollState()
    val defaultWorkTypeText = stringResource(id = R.string.other)
    val workTypes = (((screenState.uiState as? WorkdayUiState.Success)?.workTypes ?: emptyList()) + defaultWorkTypeText)
        .distinct()
        .sorted()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = padding)
            .padding(all = 24.dp),
        verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)
    ) {
        HeaderSection(
            date = screenState.date,
            workTimeByDate = workTimeByDate
        )

        ElevatedCard(
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f)
        ) {
            SingleProjectFormFields(
                scrollState = scrollState,
                screenState = screenState,
                workTypes = workTypes,
                actions = actions
            )
        }
    }
}

@Composable
private fun SingleProjectFormFields(
    scrollState: androidx.compose.foundation.ScrollState,
    screenState: SingleProjectScreenState,
    workTypes: List<String>,
    actions: SingleProjectActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollbar(scrollState = scrollState)
            .padding(all = 16.dp)
            .verticalScroll(state = scrollState),
        verticalArrangement = Arrangement.spacedBy(space = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DialogMainFields(
            state = screenState.state,
            isAddMode = screenState.isAddMode,
            isDuplicateProjectName = screenState.isDuplicateProjectName,
            onStateChange = actions.onStateChange
        )

        TimeSelectionSection(
            state = screenState.state,
            onOpenProjectDetails = actions.onOpenProjectDetails,
            onStateChange = actions.onStateChange
        )

        DialogDropdownFields(
            state = screenState.state,
            workTypeDropDownList = workTypes,
            onStateChange = actions.onStateChange
        )

        Spacer(modifier = Modifier.weight(weight = 1f))

        Button(
            onClick = actions.onConfirm,
            enabled = screenState.isConfirmEnabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.save), style = MaterialTheme.typography.titleMedium)
        }
    }
}

data class SingleProjectScreenContentParams(
    val index: Int = -1,
    val date: String,
    val state: SingleProjectState,
    val isAddMode: Boolean,
    val projectsUiState: WorkdayUiState,
    val isConfirmEnabled: Boolean,
    val hasUnsavedChanges: Boolean,
    val isDuplicateProjectName: Boolean,
    val onStateChange: (SingleProjectState) -> Unit,
    val onNavigateBack: () -> Unit,
    val onOpenProjectDetails: () -> Unit,
    val onConfirm: () -> Unit
)

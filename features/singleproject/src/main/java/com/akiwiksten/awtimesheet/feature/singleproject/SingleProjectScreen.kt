package com.akiwiksten.awtimesheet.feature.singleproject

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.FORM_SECTION_SPACING
import com.akiwiksten.awtimesheet.core.WorkTimeCalculator
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.hasChanges
import com.akiwiksten.awtimesheet.core.ui.CenteredErrorBox
import com.akiwiksten.awtimesheet.core.ui.CenteredLoadingBox
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumn
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumnState
import com.akiwiksten.awtimesheet.core.ui.UnsavedChangesDialog
import com.akiwiksten.awtimesheet.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectDropdownFieldsSection
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectHeaderSection
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectTimeSelectionSection
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectTopBar
import com.akiwiksten.awtimesheet.feature.singleproject.components.SingleProjectUpperFieldsSection
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
    val successData = (uiState as? SingleProjectUiState.Success)?.data
    val screenState = SingleProjectScreenState(
        date = successData?.date ?: "",
        editedProjectIndex = args.initialSingleProjectState.index,
        state = state,
        isAddMode = args.initialSingleProjectState.index == -1,
        uiState = uiState,
        isConfirmEnabled = derived.isConfirmEnabled,
        isDuplicateProjectName = derived.isDuplicate
    )
    val actions = SingleProjectActions(
        onStateChange = { newState ->
            val settings = (uiState as? SingleProjectUiState.Success)?.settings ?: args.initialSettings
            state = newState
                .withAbsenceLogic(state, settings, absencePrefix)
                .withFlexDayLogic(previousState = state, noAllowanceText = noAllowanceText, flexDayWorkType = flexDayWorkType)
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

@Composable
internal fun SingleProjectScreenContent(
    screenState: SingleProjectScreenState,
    actions: SingleProjectActions,
    hasUnsavedChanges: Boolean,
    onNavigateBack: () -> Unit
) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = screenState.uiState is SingleProjectUiState.Loading
    )
    val lastSuccessState = remember { mutableStateOf<SingleProjectUiState.Success?>(value = null) }
    val showUnsavedDialogState = rememberSaveable { mutableStateOf(value = false) }
    val unsavedMessage = stringResource(id = R.string.unsaved_data_message)

    LaunchedEffect(screenState.uiState) {
        if (screenState.uiState is SingleProjectUiState.Success) {
            lastSuccessState.value = screenState.uiState
        }
    }

    val guardedNavigateBack = {
        if (hasUnsavedChanges) {
            showUnsavedDialogState.value = true
        } else {
            onNavigateBack()
        }
    }

    if (showUnsavedDialogState.value) {
        UnsavedChangesDialog(
            onDismiss = { showUnsavedDialogState.value = false },
            onDiscard = onNavigateBack,
            onSave = actions.onConfirm,
            dialogText = unsavedMessage
        )
    }

    Scaffold(
        topBar = {
            SingleProjectTopSection(onNavigateBack = guardedNavigateBack)
        }
    ) { padding ->
        SingleProjectContentByUiState(
            padding = padding,
            screenState = screenState,
            actions = actions,
            showLoadingIndicator = showLoadingIndicator,
            cachedSuccessState = lastSuccessState.value
        )
    }
}

@Composable
private fun SingleProjectContentByUiState(
    padding: PaddingValues,
    screenState: SingleProjectScreenState,
    actions: SingleProjectActions,
    showLoadingIndicator: Boolean,
    cachedSuccessState: SingleProjectUiState.Success?
) {
    when (screenState.uiState) {
        is SingleProjectUiState.Loading -> SingleProjectLoadingContent(
            padding = padding,
            screenState = screenState,
            actions = actions,
            showLoadingIndicator = showLoadingIndicator,
            cachedSuccessState = cachedSuccessState
        )

        is SingleProjectUiState.Success -> SingleProjectSuccessContent(
            padding = padding,
            screenState = screenState,
            actions = actions,
            uiState = screenState.uiState
        )

        is SingleProjectUiState.Error -> CenteredErrorBox(
            errorMessage = screenState.uiState.message,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding)
        )
    }
}

@Composable
private fun SingleProjectLoadingContent(
    padding: PaddingValues,
    screenState: SingleProjectScreenState,
    actions: SingleProjectActions,
    showLoadingIndicator: Boolean,
    cachedSuccessState: SingleProjectUiState.Success?
) {
    if (showLoadingIndicator) {
        CenteredLoadingBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding)
        )
        return
    }

    SingleProjectSuccessContent(
        padding = padding,
        screenState = screenState,
        actions = actions,
        uiState = cachedSuccessState ?: SingleProjectUiState.Success(
            data = (screenState.uiState as? SingleProjectUiState.Success)?.data ?: screenState.state,
            workTimeByDate = ZERO_TIME,
            workTypes = (screenState.uiState as? SingleProjectUiState.Success)?.workTypes ?: emptyList()
        )
    )
}

@Composable
private fun SingleProjectSuccessContent(
    padding: PaddingValues,
    screenState: SingleProjectScreenState,
    actions: SingleProjectActions,
    uiState: SingleProjectUiState
) {
    val successState = uiState as? SingleProjectUiState.Success
    val originalProjectTime = successState?.data?.projectTime ?: ""
    val baseWithoutCurrent = WorkTimeCalculator.calculateFlexTime(
        initialTime = successState?.workTimeByDate ?: ZERO_TIME,
        addedTime = "-$originalProjectTime"
    )
    val workTimeByDate = WorkTimeCalculator.calculateFlexTime(
        initialTime = baseWithoutCurrent,
        addedTime = screenState.state.projectTime
    )

    SingleProjectContent(
        padding = padding,
        workTimeByDate = workTimeByDate,
        screenState = screenState.copy(uiState = uiState),
        actions = actions
    )
}

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
    val workTypes =
        (
            ((screenState.uiState as? SingleProjectUiState.Success)?.workTypes ?: emptyList()) +
                defaultWorkTypeText
            )
            .distinct()
            .sorted()

    ScrollableScreenColumn(
        state = ScrollableScreenColumnState(
            scrollState = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding),
            columnModifier = Modifier
                .fillMaxWidth()
                .padding(all = 24.dp),
            verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)
        )
    ) {
        SingleProjectHeaderSection(
            date = screenState.date,
            workTimeByDate = workTimeByDate
        )

        ElevatedCard(
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SingleProjectFormFields(
                screenState = screenState,
                workTypes = workTypes,
                actions = actions
            )
        }
    }
}

@Composable
private fun SingleProjectFormFields(
    screenState: SingleProjectScreenState,
    workTypes: List<String>,
    actions: SingleProjectActions
) {
    val flexDayWorkType = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_flex_day)
    val isFlexDay = screenState.state.workType == flexDayWorkType

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SingleProjectUpperFieldsSection(
            state = screenState.state,
            isAddMode = screenState.isAddMode,
            isDuplicateProjectName = screenState.isDuplicateProjectName,
            isFlexDay = isFlexDay,
            onStateChange = actions.onStateChange
        )

        SingleProjectTimeSelectionSection(
            state = screenState.state,
            onOpenProjectDetails = actions.onOpenProjectDetails,
            onStateChange = actions.onStateChange
        )

        SingleProjectDropdownFieldsSection(
            state = screenState.state,
            workTypeDropDownList = workTypes,
            isFlexDay = isFlexDay,
            onStateChange = actions.onStateChange
        )

        Spacer(modifier = Modifier.height(height = 8.dp))

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

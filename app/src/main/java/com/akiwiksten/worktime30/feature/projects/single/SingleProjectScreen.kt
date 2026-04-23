package com.akiwiksten.worktime30.feature.projects.single

import android.widget.Toast
import androidx.activity.compose.LocalActivity
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.FIELD_CORNER_RADIUS
import com.akiwiksten.worktime30.core.FORM_SECTION_SPACING
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.worktime30.core.ui.verticalScrollbar
import com.akiwiksten.worktime30.feature.workday.WorkdayUiState
import com.akiwiksten.worktime30.feature.workday.WorkdayViewModel
import com.akiwiksten.worktime30.feature.workday.SingleProjectState
import com.akiwiksten.worktime30.feature.projects.single.components.DialogDropdownFields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleProjectScreen(
    onNavigateBack: () -> Unit,
    onOpenProjectDetails: (SingleProjectState) -> Unit,
    initialSingleProjectState: SingleProjectState,
    viewModel: WorkdayViewModel = hiltViewModel(
        viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    )
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val savedText = stringResource(id = R.string.saved)
    val noAllowanceText = stringResource(id = R.string.no_allowance)
    val projectsUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentProjectsState = projectsUiState
    val date = (currentProjectsState as? WorkdayUiState.Success)?.date ?: ""

    val initialUiState = remember(
        initialSingleProjectState.index,
        currentProjectsState,
        initialSingleProjectState.projectTime,
        initialSingleProjectState.projectName,
        initialSingleProjectState.projectDetails,
        initialSingleProjectState.workStats,
        noAllowanceText
    ) {
        resolveInitialSingleProjectState(
            initialSingleProjectState = initialSingleProjectState,
            projectsUiState = currentProjectsState
        ).withDefaultAllowance(defaultAllowance = noAllowanceText)
    }

    var state by remember(initialUiState) { mutableStateOf(value = initialUiState) }

    // Update state when the ViewModel data changes (e.g., returning from ProjectDetailsScreen)
    LaunchedEffect(initialUiState) {
        state = initialUiState
    }

    val isConfirmEnabled by remember(state, initialUiState) {
        derivedStateOf {
            state.projectName.isNotBlank() &&
                state.kilometres.isDigitsOnly() &&
                state != initialUiState
        }
    }

    SingleProjectScreenContent(
        params = SingleProjectScreenContentParams(
            index = initialSingleProjectState.index,
            date = date,
            state = state,
            isAddMode = initialSingleProjectState.index == -1,
            projectsUiState = currentProjectsState,
            isConfirmEnabled = isConfirmEnabled,
            onStateChange = { state = it },
            onNavigateBack = onNavigateBack,
            onOpenProjectDetails = { onOpenProjectDetails(state) },
            onConfirm = {
                viewModel.saveProject(state = state)
                Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
        )
    )
}

private fun SingleProjectState.withDefaultAllowance(defaultAllowance: String): SingleProjectState {
    return if (allowance.isBlank()) copy(allowance = defaultAllowance) else this
}

internal fun resolveInitialSingleProjectState(
    initialSingleProjectState: SingleProjectState,
    projectsUiState: WorkdayUiState
): SingleProjectState {
    val hasNavigationPayload = initialSingleProjectState.projectName.isNotBlank() ||
        initialSingleProjectState.projectTime != ZERO_TIME ||
        initialSingleProjectState.projectDetails != null ||
        initialSingleProjectState.workStats != null

    val resolvedState = when {
        initialSingleProjectState.index == -1 || hasNavigationPayload -> initialSingleProjectState
        else -> (projectsUiState as? WorkdayUiState.Success)
            ?.projects
            ?.find { it.index == initialSingleProjectState.index }
            ?: initialSingleProjectState
    }

    return resolvedState
}

@Composable
internal fun SingleProjectScreenContent(params: SingleProjectScreenContentParams) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = params.projectsUiState is WorkdayUiState.Loading
    )
    var lastSuccessState by remember { mutableStateOf<WorkdayUiState.Success?>(value = null) }

    LaunchedEffect(params.projectsUiState) {
        if (params.projectsUiState is WorkdayUiState.Success) {
            lastSuccessState = params.projectsUiState
        }
    }

    val actions = SingleProjectActions(
        onStateChange = params.onStateChange,
        onOpenProjectDetails = params.onOpenProjectDetails,
        onConfirm = params.onConfirm
    )

    Scaffold(
        topBar = {
            SingleProjectTopSection(
                onNavigateBack = params.onNavigateBack
            )
        }
    ) { padding ->
        when (params.projectsUiState) {
            is WorkdayUiState.Loading -> SingleProjectLoadingContent(
                padding = padding,
                params = params,
                actions = actions,
                showLoadingIndicator = showLoadingIndicator,
                cachedSuccessState = lastSuccessState
            )
            is WorkdayUiState.Success -> SingleProjectSuccessContent(
                padding = padding,
                params = params,
                actions = actions,
                uiState = params.projectsUiState
            )
            is WorkdayUiState.Error -> SingleProjectErrorContent(
                padding = padding,
                message = params.projectsUiState.message
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
    val baseWithoutCurrent = WorkTimeCalculator.calculateWorkTimeBalance(
        initialTime = successState?.workTimeToday ?: ZERO_TIME,
        addedTime = "-$originalProjectTime"
    )
    val workTimeToday = WorkTimeCalculator.calculateWorkTimeBalance(
        initialTime = baseWithoutCurrent,
        addedTime = params.state.projectTime
    )

    SingleProjectContent(
        padding = padding,
        workTimeToday = workTimeToday,
        screenState = SingleProjectScreenState(
            date = params.date,
            editedProjectIndex = params.index,
            state = params.state,
            isAddMode = params.isAddMode,
            uiState = uiState,
            isConfirmEnabled = params.isConfirmEnabled
        ),
        actions = actions
    )
}

@Composable
private fun SingleProjectErrorContent(padding: PaddingValues, message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Error: $message",
            color = MaterialTheme.colorScheme.error
        )
    }
}

data class SingleProjectScreenState(
    val date: String,
    val editedProjectIndex: Int,
    val state: SingleProjectState,
    val isAddMode: Boolean,
    val uiState: WorkdayUiState,
    val isConfirmEnabled: Boolean
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
    workTimeToday: String,
    screenState: SingleProjectScreenState,
    actions: SingleProjectActions
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = padding)
            .padding(all = 24.dp),
        verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)
    ) {
        HeaderSection(
            date = screenState.date,
            workTimeToday = workTimeToday
        )

        ElevatedCard(
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f)
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
                    onStateChange = actions.onStateChange
                )

                TimeSelectionSection(
                    state = screenState.state,
                    onOpenProjectDetails = actions.onOpenProjectDetails,
                    onStateChange = actions.onStateChange
                )

                DialogDropdownFields(
                    state = screenState.state,
                    workTypeDropDownList = (screenState.uiState as? WorkdayUiState.Success)?.workTypes
                        ?: emptyList(),
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
    }
}

data class SingleProjectScreenContentParams(
    val index: Int = -1,
    val date: String,
    val state: SingleProjectState,
    val isAddMode: Boolean,
    val projectsUiState: WorkdayUiState,
    val isConfirmEnabled: Boolean,
    val onStateChange: (SingleProjectState) -> Unit,
    val onNavigateBack: () -> Unit,
    val onOpenProjectDetails: () -> Unit,
    val onConfirm: () -> Unit
)

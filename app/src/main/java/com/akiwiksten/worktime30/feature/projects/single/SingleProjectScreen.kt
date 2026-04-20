package com.akiwiksten.worktime30.feature.projects.single

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.worktime30.feature.projects.daily.ProjectsUiState
import com.akiwiksten.worktime30.feature.projects.daily.ProjectsViewModel
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState
import com.akiwiksten.worktime30.feature.projects.single.components.DialogDropdownFields

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleProjectScreen(
    onNavigateBack: () -> Unit,
    onOpenProjectDetails: (SingleProjectState) -> Unit,
    initialSingleProjectState: SingleProjectState,
    viewModel: ProjectsViewModel = hiltViewModel(
        viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    )
) {
    val projectsUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentProjectsState = projectsUiState
    val date = (currentProjectsState as? ProjectsUiState.Success)?.date ?: ""

    val initialUiState = remember(
        initialSingleProjectState.index,
        currentProjectsState,
        initialSingleProjectState.projectTime,
        initialSingleProjectState.projectName
    ) {
        if (initialSingleProjectState.index != -1 && currentProjectsState is ProjectsUiState.Success) {
            currentProjectsState
                .projects
                .find {
                    it.index == initialSingleProjectState.index
                } ?: SingleProjectState()
            // If we have initial values from navigation (e.g., after returning from ProjectDetailsScreen), use them
        } else {
            initialSingleProjectState
        }
    }

    var state by remember(initialUiState) { mutableStateOf(value = initialUiState) }

    // Update state when the ViewModel data changes (e.g., returning from ProjectDetailsScreen)
    LaunchedEffect(initialUiState) {
        state = initialUiState
    }

    val isConfirmEnabled by remember {
        derivedStateOf {
            state.projectName.isNotBlank() && state.kilometres.isDigitsOnly()
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
                onNavigateBack()
            }
        )
    )
}

@Composable
internal fun SingleProjectScreenContent(params: SingleProjectScreenContentParams) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = params.projectsUiState is ProjectsUiState.Loading
    )
    var lastSuccessState by remember { mutableStateOf<ProjectsUiState.Success?>(value = null) }

    LaunchedEffect(params.projectsUiState) {
        if (params.projectsUiState is ProjectsUiState.Success) {
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
            is ProjectsUiState.Loading -> SingleProjectLoadingContent(
                padding = padding,
                params = params,
                actions = actions,
                showLoadingIndicator = showLoadingIndicator,
                cachedSuccessState = lastSuccessState
            )
            is ProjectsUiState.Success -> SingleProjectSuccessContent(
                padding = padding,
                params = params,
                actions = actions,
                uiState = params.projectsUiState
            )
            is ProjectsUiState.Error -> SingleProjectErrorContent(
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
    cachedSuccessState: ProjectsUiState.Success?
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
        uiState = cachedSuccessState ?: ProjectsUiState.Success(date = params.date)
    )
}

@Composable
private fun SingleProjectSuccessContent(
    padding: PaddingValues,
    params: SingleProjectScreenContentParams,
    actions: SingleProjectActions,
    uiState: ProjectsUiState
) {
    SingleProjectContent(
        padding = padding,
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
    val uiState: ProjectsUiState,
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
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            SingleProjectTopBar(onNavigateBack = onNavigateBack)

        }
    }
}

@Composable
private fun SingleProjectContent(
    padding: PaddingValues,
    screenState: SingleProjectScreenState,
    actions: SingleProjectActions
) {
    val successState = screenState.uiState as? ProjectsUiState.Success
    val originalProjectTime = successState
        ?.projects
        ?.find { it.index == screenState.editedProjectIndex }
        ?.projectTime
        ?: ZERO_TIME
    val baseWithoutCurrent = WorkTimeCalculator.calculateWorkTimeBalance(
        initialTime = successState?.workTimeToday ?: ZERO_TIME,
        addedTime = "-$originalProjectTime"
    )
    val workTimeToday = WorkTimeCalculator.calculateWorkTimeBalance(
        initialTime = baseWithoutCurrent,
        addedTime = screenState.state.projectTime
    )

    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = padding)
            .padding(all = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 16.dp)
                .verticalScroll(state = rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(space = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeaderSection(date = screenState.date)

            DialogMainFields(
                state = screenState.state,
                isAddMode = screenState.isAddMode,
                onStateChange = actions.onStateChange
            )

            TimeSelectionSection(
                state = screenState.state,
                workTimeToday = workTimeToday,
                onOpenProjectDetails = actions.onOpenProjectDetails,
                onStateChange = actions.onStateChange
            )

            DialogDropdownFields(
                state = screenState.state,
                workTypeDropDownList = (screenState.uiState as? ProjectsUiState.Success)?.workTypes
                    ?: emptyList(),
                onStateChange = actions.onStateChange
            )

            Spacer(modifier = Modifier.weight(weight = 1f))

            Button(
                onClick = actions.onConfirm,
                enabled = screenState.isConfirmEnabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(size = 12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.save), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun DialogMainFields(
    state: SingleProjectState,
    isAddMode: Boolean,
    onStateChange: (SingleProjectState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
        state.projectName.let {
            OutlinedTextField(
                value = it,
                onValueChange = { onStateChange(state.copy(projectName = it)) },
                label = { Text(text = stringResource(id = R.string.project_name)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isAddMode,
                singleLine = true,
                shape = RoundedCornerShape(size = 12.dp)
            )
        }

        state.kilometres.let {
            OutlinedTextField(
                value = it,
                onValueChange = { if (it.isDigitsOnly()) onStateChange(state.copy(kilometres = it)) },
                label = { Text(text = stringResource(id = R.string.kilometres)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(size = 12.dp)
            )
        }
    }
}

@Composable
private fun TimeSelectionSection(
    state: SingleProjectState,
    workTimeToday: String,
    onOpenProjectDetails: () -> Unit,
    onStateChange: (SingleProjectState) -> Unit
) {
    val openTimePickerDialogState = remember { mutableStateOf(false) }

    ProjectTimePickerDialog(
        showDialog = openTimePickerDialogState.value,
        onDismissRequest = { openTimePickerDialogState.value = false },
        onConfirmation = { time ->
            onStateChange(state.copy(projectTime = time))
            openTimePickerDialogState.value = false
        },
        currentTime = state.projectTime
    )

    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Text(
            text = "${stringResource(id = R.string.work_time_today)}: $workTimeToday",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            state.projectTime.let {
                OutlinedTextField(
                    value = it,
                    onValueChange = { onStateChange(state.copy(projectTime = it)) },
                    label = { Text(text = stringResource(id = R.string.project_time)) },
                    modifier = Modifier.weight(weight = 1f),
                    readOnly = true,
                    leadingIcon = { Icon(imageVector = Icons.Default.AccessTime, contentDescription = null) },
                    shape = RoundedCornerShape(size = 12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            Button(
                onClick = onOpenProjectDetails,
                modifier = Modifier.padding(top = 8.dp),
                shape = RoundedCornerShape(size = 12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = null)
                Spacer(modifier = Modifier.width(width = 4.dp))
                Text(text = stringResource(id = R.string.details))
            }

            Button(
                onClick = { openTimePickerDialogState.value = true },
                modifier = Modifier.padding(top = 8.dp),
                shape = RoundedCornerShape(size = 12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = stringResource(id = R.string.go_to_time_picker)
                )
            }
        }
    }
}

data class SingleProjectScreenContentParams(
    val index: Int = -1,
    val date: String,
    val state: SingleProjectState,
    val isAddMode: Boolean,
    val projectsUiState: ProjectsUiState,
    val isConfirmEnabled: Boolean,
    val onStateChange: (SingleProjectState) -> Unit,
    val onNavigateBack: () -> Unit,
    val onOpenProjectDetails: () -> Unit,
    val onConfirm: () -> Unit
)

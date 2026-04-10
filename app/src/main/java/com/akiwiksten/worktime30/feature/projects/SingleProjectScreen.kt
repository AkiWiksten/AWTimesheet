package com.akiwiksten.worktime30.feature.projects

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.DropdownMenuBox
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.feature.calendar.CalendarUiState
import com.akiwiksten.worktime30.feature.calendar.CalendarViewModel

data class SingleProjectArgs(
    val index: Int,
    val projectName: String? = null,
    val workTime: String? = null,
    val kilometres: String? = null,
    val allowance: String? = null,
    val workType: String? = null,
    val workday: WorkdayEntity? = null,
    val workStats: WorkStatsEntity? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleProjectScreen(
    args: SingleProjectArgs,
    onNavigateBack: () -> Unit,
    onOpenWorkday: (ProjectDialogState) -> Unit,
    calendarViewModel: CalendarViewModel = hiltViewModel(
        viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    ),
    viewModel: ProjectsViewModel = hiltViewModel(
        viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    )
) {
    val projectsUiState by viewModel.uiState.collectAsState()
    val calendarUiState by calendarViewModel.uiState.collectAsState()
    val currentCalendarState = calendarUiState // Store in local variable for smart cast
    val date = (currentCalendarState as? CalendarUiState.Success)?.date ?: ""
    val currentProjectsState = projectsUiState // Store in local variable for smart cast

    LaunchedEffect(key1 = date) {
        if (date.isNotEmpty()) {
            viewModel.loadData(date = date)
        }
    }

    val initialUiState = remember(args.index, currentProjectsState) {
        if (args.index != -1 && currentProjectsState is ProjectsUiState.Success) {
            currentProjectsState.projects.find { it.index == args.index } ?: ProjectListItemUiState()
        } else {
            ProjectListItemUiState()
        }
    }

    var state by remember(initialUiState) { mutableStateOf(value = ProjectDialogState(uiState = initialUiState)) }

    LaunchedEffect(key1 = initialUiState, key2 = args) {
        args.projectName?.let { state = state.copy(projectName = it) }
        args.workTime?.let { state = state.copy(projectTime = it) }
        args.kilometres?.let { state = state.copy(kilometres = it) }
        args.allowance?.let { state = state.copy(allowance = it) }
        args.workType?.let { state = state.copy(workType = it) }
        args.workday?.let { state = state.copy(workday = it) }
        args.workStats?.let { state = state.copy(workStats = it) }
    }

    val isConfirmEnabled by remember {
        derivedStateOf {
            state.projectName.isNotBlank() && state.kilometres.isDigitsOnly()
        }
    }

    SingleProjectScreenContent(
        params = SingleProjectScreenContentParams(
            date = date,
            state = state,
            isAddMode = args.index == -1,
            projectsUiState = currentProjectsState,
            isConfirmEnabled = isConfirmEnabled,
            onStateChange = { state = it },
            onNavigateBack = onNavigateBack,
            onOpenWorkday = { onOpenWorkday(state) },
            onConfirm = {
                viewModel.saveProject(uiState = state.toUiState())
                onNavigateBack()
            }
        )
    )
}

@Composable
internal fun SingleProjectScreenContent(params: SingleProjectScreenContentParams) {
    Scaffold(
        topBar = { SingleProjectTopBar(onNavigateBack = params.onNavigateBack) }
    ) { padding ->
        when (params.projectsUiState) {
            is ProjectsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is ProjectsUiState.Success -> {
                SingleProjectContent(
                    padding = padding,
                    screenState = SingleProjectScreenState(
                        date = params.date,
                        state = params.state,
                        isAddMode = params.isAddMode,
                        uiState = params.projectsUiState,
                        isConfirmEnabled = params.isConfirmEnabled
                    ),
                    actions = SingleProjectActions(
                        onStateChange = params.onStateChange,
                        onOpenWorkday = params.onOpenWorkday,
                        onConfirm = params.onConfirm
                    )
                )
            }
            is ProjectsUiState.Error -> {
                Box(
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SingleProjectTopBar(onNavigateBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Header(
                title = stringResource(id = R.string.project_customer),
                modifier = Modifier.padding(top = 0.dp)
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        },
        actions = {
            Spacer(modifier = Modifier.width(width = 48.dp))
        }
    )
}

data class SingleProjectScreenState(
    val date: String,
    val state: ProjectDialogState,
    val isAddMode: Boolean,
    val uiState: ProjectsUiState,
    val isConfirmEnabled: Boolean
)

data class SingleProjectActions(
    val onStateChange: (ProjectDialogState) -> Unit,
    val onOpenWorkday: () -> Unit,
    val onConfirm: () -> Unit
)

@Composable
private fun SingleProjectContent(
    padding: PaddingValues,
    screenState: SingleProjectScreenState,
    actions: SingleProjectActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = padding)
            .padding(all = 24.dp)
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
            workTimeToday = (screenState.uiState as? ProjectsUiState.Success)?.workTimeToday ?: "",
            onOpenWorkday = actions.onOpenWorkday,
            onStateChange = actions.onStateChange
        )

        DialogDropdownFields(
            state = screenState.state,
            workTypeDropDownList = (screenState.uiState as? ProjectsUiState.Success)?.workTypes ?: emptyList(),
            onStateChange = actions.onStateChange
        )

        Spacer(modifier = Modifier.weight(weight = 1f))

        Button(
            onClick = actions.onConfirm,
            enabled = screenState.isConfirmEnabled,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(size = 12.dp)
        ) {
            Text(text = stringResource(id = R.string.save), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun HeaderSection(date: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DialogMainFields(
    state: ProjectDialogState,
    isAddMode: Boolean,
    onStateChange: (ProjectDialogState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
        OutlinedTextField(
            value = state.projectName,
            onValueChange = { onStateChange(state.copy(projectName = it)) },
            label = { Text(text = stringResource(id = R.string.project_name)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = isAddMode,
            singleLine = true,
            shape = RoundedCornerShape(size = 12.dp)
        )

        OutlinedTextField(
            value = state.kilometres,
            onValueChange = { if (it.isDigitsOnly()) onStateChange(state.copy(kilometres = it)) },
            label = { Text(text = stringResource(id = R.string.kilometres)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(size = 12.dp)
        )
    }
}

@Composable
private fun TimeSelectionSection(
    state: ProjectDialogState,
    workTimeToday: String,
    onOpenWorkday: () -> Unit,
    onStateChange: (ProjectDialogState) -> Unit
) {
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
            OutlinedTextField(
                value = state.projectTime,
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

            Button(
                onClick = onOpenWorkday,
                modifier = Modifier.padding(top = 8.dp),
                shape = RoundedCornerShape(size = 12.dp)
            ) {
                Icon(imageVector = Icons.Default.History, contentDescription = null)
                Spacer(modifier = Modifier.width(width = 4.dp))
                Text(text = stringResource(id = R.string.confirm))
            }
        }
    }
}

@Composable
private fun DialogDropdownFields(
    state: ProjectDialogState,
    workTypeDropDownList: List<String>,
    onStateChange: (ProjectDialogState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
        DropdownMenuBox(
            labelId = R.string.work_type,
            items = workTypeDropDownList,
            selectedText = state.workType,
            onItemSelected = { onStateChange(state.copy(workType = it)) }
        )

        DropdownMenuBox(
            labelId = R.string.allowance,
            items = listOf("No allowance", "Full allowance", "Half allowance"),
            selectedText = state.allowance,
            onItemSelected = { onStateChange(state.copy(allowance = it)) }
        )
    }
}

data class SingleProjectScreenContentParams(
    val date: String,
    val state: ProjectDialogState,
    val isAddMode: Boolean,
    val projectsUiState: ProjectsUiState,
    val isConfirmEnabled: Boolean,
    val onStateChange: (ProjectDialogState) -> Unit,
    val onNavigateBack: () -> Unit,
    val onOpenWorkday: () -> Unit,
    val onConfirm: () -> Unit
)

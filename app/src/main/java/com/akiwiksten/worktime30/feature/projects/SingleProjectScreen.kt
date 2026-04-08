package com.akiwiksten.worktime30.feature.projects

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.DropdownMenuBox
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
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
    calendarViewModel: CalendarViewModel = hiltViewModel(viewModelStoreOwner = LocalContext.current.findActivity()),
    viewModel: ProjectsViewModel = hiltViewModel(viewModelStoreOwner = LocalContext.current.findActivity())
) {
    val projectsUiState by viewModel.uiState.collectAsState()
    val calendarUiState by calendarViewModel.uiState.collectAsState()
    val date = calendarUiState.date

    LaunchedEffect(key1 = date) {
        if (date.isNotEmpty()) {
            viewModel.loadData(date = date)
        }
    }

    val initialUiState = remember(args.index, projectsUiState.projects) {
        if (args.index != -1) {
            projectsUiState.projects.find { it.index == args.index } ?: ProjectListItemUiState()
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

    Scaffold(
        topBar = { SingleProjectTopBar(onNavigateBack = onNavigateBack) }
    ) { padding ->
        SingleProjectContent(
            padding = padding,
            screenState = SingleProjectScreenState(
                date = date,
                state = state,
                isAddMode = args.index == -1,
                uiState = projectsUiState,
                isConfirmEnabled = isConfirmEnabled
            ),
            actions = SingleProjectActions(
                onStateChange = { state = it },
                onOpenWorkday = { onOpenWorkday(state) },
                onConfirm = {
                    viewModel.saveProject(uiState = state.toUiState())
                    onNavigateBack()
                }
            )
        )
    }
}

private fun Context.findActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    error("Context does not have an Activity")
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
            workTimeToday = screenState.uiState.workTimeToday,
            onOpenWorkday = actions.onOpenWorkday,
            onStateChange = actions.onStateChange
        )

        DialogDropdownFields(
            state = screenState.state,
            workTypeDropDownList = screenState.uiState.workTypes,
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
    Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        CompactTimeRow(
            labelId = R.string.work_time,
            value = state.projectTime,
            onOpenWorkday = onOpenWorkday,
            onHistoryClick = {
                onStateChange(state.copy(projectTime = workTimeToday))
            }
        )
    }
}

@Composable
private fun DialogDropdownFields(
    state: ProjectDialogState,
    workTypeDropDownList: List<String>,
    onStateChange: (ProjectDialogState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        DropdownMenuBox(
            items = listOf(
                stringResource(id = R.string.no_allowance),
                stringResource(id = R.string.daily_allowance),
                stringResource(id = R.string.half_day_allowance)
            ),
            onItemSelected = { onStateChange(state.copy(allowance = it)) },
            labelId = R.string.allowance,
            selectedText = state.allowance,
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenuBox(
            items = workTypeDropDownList,
            onItemSelected = { onStateChange(state.copy(workType = it)) },
            labelId = R.string.work_type,
            selectedText = state.workType,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CompactTimeRow(
    labelId: Int,
    value: String,
    onOpenWorkday: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(text = stringResource(id = labelId)) },
            readOnly = true,
            modifier = Modifier.weight(weight = 1f),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
            ),
            shape = RoundedCornerShape(size = 12.dp)
        )

        IconButton(onClick = onHistoryClick) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = onOpenWorkday) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@file:Suppress("TooManyFunctions")

package com.akiwiksten.worktime30.feature.projects.daily

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.TimePickerDialog
import com.akiwiksten.worktime30.core.ui.rememberDelayedLoadingVisibility

@Suppress("kotlin:S1854", "UNUSED_VALUE")
@Composable
fun ProjectsScreen(
    onNavigateToSingleProject: (Int) -> Unit,
    projectsViewModel: ProjectsViewModel = hiltViewModel(
        viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    ),
) {
    val projectsUiState by projectsViewModel.uiState.collectAsState()

    // Use state object directly to avoid SonarQube "unused assignment" false positives with 'by' delegate
    val selectedItemIndexState = remember { mutableIntStateOf(value = -1) }

    ProjectsContent(
        projectsUiState = projectsUiState,
        selectedItemIndex = selectedItemIndexState.intValue,
        actions = ProjectsActions(
            onSelectedItemIndexChange = { selectedItemIndexState.intValue = it },
            onNavigateToSingleProject = onNavigateToSingleProject,
            onRetry = projectsViewModel::retryLoad,
            onSaveWorkStats = projectsViewModel::updateWorkStats,
            onDeleteProject = { project ->
                projectsViewModel.deleteProject(state = project)
                selectedItemIndexState.intValue = -1
            }
        )
    )
}

@Composable
internal fun ProjectsContent(
    projectsUiState: ProjectsUiState,
    selectedItemIndex: Int,
    actions: ProjectsActions
) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = projectsUiState is ProjectsUiState.Loading
    )
    var lastSuccessState by remember { mutableStateOf<ProjectsUiState.Success?>(value = null) }

    LaunchedEffect(projectsUiState) {
        if (projectsUiState is ProjectsUiState.Success) {
            lastSuccessState = projectsUiState
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {
        when (projectsUiState) {
            is ProjectsUiState.Loading -> ProjectsLoadingContent(
                showLoadingIndicator = showLoadingIndicator,
                cachedState = lastSuccessState,
                selectedItemIndex = selectedItemIndex,
                actions = actions
            )
            is ProjectsUiState.Success -> ProjectsSuccessContent(
                state = projectsUiState,
                selectedItemIndex = selectedItemIndex,
                actions = actions
            )
            is ProjectsUiState.Error -> ProjectsErrorContent(
                message = projectsUiState.message,
                onRetry = actions.onRetry
            )
        }
    }
}

@Composable
private fun ColumnScope.ProjectsLoadingContent(
    showLoadingIndicator: Boolean,
    cachedState: ProjectsUiState.Success?,
    selectedItemIndex: Int,
    actions: ProjectsActions
) {
    if (showLoadingIndicator) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    cachedState?.let {
        ProjectsSuccessContent(state = it, selectedItemIndex = selectedItemIndex, actions = actions)
    }
}

@Composable
private fun ColumnScope.ProjectsSuccessContent(
    state: ProjectsUiState.Success,
    selectedItemIndex: Int,
    actions: ProjectsActions
) {
    val saveUi = rememberProjectsSaveUi(
        initialDailyWorkTime = state.dailyWorkTime,
        initialBalanceTotal = state.balanceTotal,
        onSaveWorkStats = actions.onSaveWorkStats
    )

    ProjectsHeader(
        date = state.date,
        workTime = state.workTimeToday,
        balanceToday = state.balanceToday,
        workStatsEditorState = WorkStatsEditorState(
            dailyWorkTime = saveUi.dailyWorkTime,
            balanceTotal = saveUi.balanceTotal,
            isDailyWorkTimeError = !saveUi.isDailyWorkTimeValid,
            isBalanceTotalError = !saveUi.isBalanceTotalValid,
            hasUnsavedChanges = saveUi.hasUnsavedChanges
        ),
        headerActions = ProjectsHeaderActions(
            onDailyWorkTimeChange = saveUi.onDailyWorkTimeChange,
            onBalanceTotalChange = saveUi.onBalanceTotalChange,
            onSaveWorkStats = saveUi.onSaveRequested
        )
    )

    ProjectsListSection(
        items = state.projects,
        selectedIndex = selectedItemIndex,
        onItemSelected = actions.onSelectedItemIndexChange,
        modifier = Modifier.weight(weight = 1f)
    )

    ProjectsActionButtons(
        items = state.projects,
        selectedIndex = selectedItemIndex,
        onAddClick = { actions.onNavigateToSingleProject(-1) },
        onEditClick = { actions.onNavigateToSingleProject(selectedItemIndex) },
        onDeleteClick = {
            state.projects.getOrNull(index = selectedItemIndex)?.let(actions.onDeleteProject)
        }
    )
}

@Composable
private fun rememberProjectsSaveUi(
    initialDailyWorkTime: String,
    initialBalanceTotal: String,
    onSaveWorkStats: (String, String) -> Unit
): ProjectsSaveUi {
    val context = androidx.compose.ui.platform.LocalContext.current
    val savedText = stringResource(id = R.string.saved)
    var editedDailyWorkTime by remember(initialDailyWorkTime) {
        mutableStateOf(value = initialDailyWorkTime)
    }
    var editedBalanceTotal by remember(initialBalanceTotal) {
        mutableStateOf(value = initialBalanceTotal)
    }
    val lastSavedDailyWorkTimeState = remember(initialDailyWorkTime) {
        mutableStateOf(value = initialDailyWorkTime)
    }
    val lastSavedBalanceTotalState = remember(initialBalanceTotal) {
        mutableStateOf(value = initialBalanceTotal)
    }

    val isDailyWorkTimeValid = remember(editedDailyWorkTime) {
        editedDailyWorkTime.matches(DAILY_WORK_TIME_INPUT_REGEX)
    }
    val isBalanceTotalValid = remember(editedBalanceTotal) {
        editedBalanceTotal.matches(BALANCE_TOTAL_INPUT_REGEX)
    }
    val hasUnsavedChanges =
        editedDailyWorkTime != lastSavedDailyWorkTimeState.value ||
            editedBalanceTotal != lastSavedBalanceTotalState.value

    return ProjectsSaveUi(
        dailyWorkTime = editedDailyWorkTime,
        balanceTotal = editedBalanceTotal,
        isDailyWorkTimeValid = isDailyWorkTimeValid,
        isBalanceTotalValid = isBalanceTotalValid,
        hasUnsavedChanges = hasUnsavedChanges,
        onDailyWorkTimeChange = {
            editedDailyWorkTime = it
        },
        onBalanceTotalChange = {
            editedBalanceTotal = it
        },
        onSaveRequested = {
            if (isDailyWorkTimeValid && isBalanceTotalValid && hasUnsavedChanges) {
                onSaveWorkStats(editedDailyWorkTime, editedBalanceTotal)
                lastSavedDailyWorkTimeState.value = editedDailyWorkTime
                lastSavedBalanceTotalState.value = editedBalanceTotal
                Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@Composable
private fun ProjectsErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error: $message",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(all = 32.dp)
        )
        Button(onClick = onRetry) {
            Text(text = stringResource(id = R.string.retry))
        }
    }
}

@Composable
private fun ProjectsHeader(
    date: String,
    workTime: String,
    balanceToday: String,
    workStatsEditorState: WorkStatsEditorState,
    headerActions: ProjectsHeaderActions
) {
    val openDailyWorkTimePicker = remember { mutableStateOf(value = false) }

    if (openDailyWorkTimePicker.value) {
        TimePickerDialog(
            onDismissRequest = { openDailyWorkTimePicker.value = false },
            onConfirmation = { time ->
                headerActions.onDailyWorkTimeChange(time)
                openDailyWorkTimePicker.value = false
            },
            time = workStatsEditorState.dailyWorkTime,
            titleId = R.string.daily_work_time
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        ProjectsHeaderTitleCard(date = date)
        ProjectsHeaderStatsCard(
            workTime = workTime,
            balanceToday = balanceToday,
            workStatsEditorState = workStatsEditorState,
            onDailyWorkTimePickerClick = { openDailyWorkTimePicker.value = true },
            onBalanceTotalChange = headerActions.onBalanceTotalChange,
            onSaveWorkStats = headerActions.onSaveWorkStats
        )
    }
}

@Composable
private fun ProjectsHeaderTitleCard(date: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            Header(title = stringResource(id = R.string.projects_customers))
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ProjectsHeaderStatsCard(
    workTime: String,
    balanceToday: String,
    workStatsEditorState: WorkStatsEditorState,
    onDailyWorkTimePickerClick: () -> Unit,
    onBalanceTotalChange: (String) -> Unit,
    onSaveWorkStats: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            Text(
                text = "${stringResource(id = R.string.work_time_today)}: $workTime",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "${stringResource(id = R.string.balance_today)}: $balanceToday",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DailyWorkTimePickerRow(
                dailyWorkTime = workStatsEditorState.dailyWorkTime,
                isError = workStatsEditorState.isDailyWorkTimeError,
                onPickerClick = onDailyWorkTimePickerClick
            )
            OutlinedTextField(
                value = workStatsEditorState.balanceTotal,
                onValueChange = onBalanceTotalChange,
                label = { Text(text = stringResource(id = R.string.balance_total)) },
                singleLine = true,
                isError = workStatsEditorState.isBalanceTotalError,
                modifier = Modifier.fillMaxWidth()
            )
            SaveWorkStatsButton(
                isEnabled = !workStatsEditorState.isDailyWorkTimeError &&
                    !workStatsEditorState.isBalanceTotalError &&
                    workStatsEditorState.hasUnsavedChanges,
                onClick = onSaveWorkStats,
                modifier = Modifier.align(alignment = Alignment.End)
            )
        }
    }
}

@Composable
private fun DailyWorkTimePickerRow(
    dailyWorkTime: String,
    isError: Boolean,
    onPickerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        OutlinedTextField(
            value = dailyWorkTime,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = stringResource(id = R.string.daily_work_time)) },
            singleLine = true,
            isError = isError,
            modifier = Modifier.weight(weight = 1f)
        )
        IconButton(onClick = onPickerClick) {
            Icon(imageVector = Icons.Default.AccessTime, contentDescription = null)
        }
    }
}

@Composable
private fun SaveWorkStatsButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Icon(imageVector = Icons.Default.Save, contentDescription = null)
        Spacer(modifier = Modifier.width(width = 8.dp))
        Text(text = stringResource(id = R.string.save))
    }
}

@Composable
private fun ProjectsListSection(
    items: List<SingleProjectState>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.no_projects_available),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(all = 32.dp)
            )
        }
    } else {
        ElevatedCard(
            modifier = modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(size = 12.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(size = 12.dp)
                    )
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(space = 2.dp)
            ) {
                items(
                    items = items,
                    key = { it.projectName }
                ) { item ->
                    ProjectListItem(
                        item = item,
                        isSelected = selectedIndex == item.index,
                        onClick = { onItemSelected(item.index) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectListItem(
    item: SingleProjectState,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onClick),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.projectName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = item.projectTime,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProjectDetails(
                    workType = item.workType,
                    allowance = item.allowance,
                    modifier = Modifier.weight(weight = 1f)
                )
                Text(
                    text = "${item.kilometres} km",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ProjectDetails(workType: String, allowance: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        if (workType.isNotEmpty()) {
            Text(
                text = workType,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (allowance.isNotEmpty()) {
            Text(
                text = allowance,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun ProjectsActionButtons(
    items: List<SingleProjectState>,
    selectedIndex: Int,
    onAddClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val selectedProject = items.getOrNull(index = selectedIndex)
    val deleteButtonText = if (selectedProject?.projectTime != ZERO_TIME) {
        stringResource(id = R.string.nullify)
    } else {
        stringResource(id = R.string.delete)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Button(
            onClick = onAddClick,
            modifier = Modifier.weight(weight = 1f),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(size = 18.dp))
            Spacer(modifier = Modifier.width(width = 8.dp))
            Text(text = stringResource(id = R.string.add))
        }
        Button(
            onClick = onEditClick,
            enabled = selectedIndex != -1,
            modifier = Modifier.weight(weight = 1f),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(size = 18.dp))
            Spacer(modifier = Modifier.width(width = 8.dp))
            Text(text = stringResource(id = R.string.edit))
        }
        Button(
            onClick = onDeleteClick,
            enabled = selectedIndex != -1,
            modifier = Modifier.weight(weight = 1f),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(size = 18.dp))
            Spacer(modifier = Modifier.width(width = 8.dp))
            Text(text = deleteButtonText)
        }
    }
}

data class ProjectsActions(
    val onSelectedItemIndexChange: (Int) -> Unit,
    val onNavigateToSingleProject: (Int) -> Unit,
    val onRetry: () -> Unit,
    val onSaveWorkStats: (String, String) -> Unit,
    val onDeleteProject: (SingleProjectState) -> Unit
)

private data class WorkStatsEditorState(
    val dailyWorkTime: String,
    val balanceTotal: String,
    val isDailyWorkTimeError: Boolean,
    val isBalanceTotalError: Boolean,
    val hasUnsavedChanges: Boolean
)

private data class ProjectsHeaderActions(
    val onDailyWorkTimeChange: (String) -> Unit,
    val onBalanceTotalChange: (String) -> Unit,
    val onSaveWorkStats: () -> Unit
)

private data class ProjectsSaveUi(
    val dailyWorkTime: String,
    val balanceTotal: String,
    val isDailyWorkTimeValid: Boolean,
    val isBalanceTotalValid: Boolean,
    val hasUnsavedChanges: Boolean,
    val onDailyWorkTimeChange: (String) -> Unit,
    val onBalanceTotalChange: (String) -> Unit,
    val onSaveRequested: () -> Unit
)

private val DAILY_WORK_TIME_INPUT_REGEX = Regex(pattern = "(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")
private val BALANCE_TOTAL_INPUT_REGEX = Regex(pattern = "[+-]?(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")

package com.akiwiksten.worktime30.feature.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.AddTextFieldDialog
import com.akiwiksten.worktime30.core.ui.DropdownMenuBox
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.rememberDelayedLoadingVisibility

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        settingsViewModel.loadSettings()
    }

    SettingsStateContent(
        uiState = uiState,
        createActions = { successState ->
            createSettingsActions(
                settingsViewModel = settingsViewModel,
                successState = successState,
                ctx = ctx
            )
        }
    )
}

@Composable
internal fun SettingsStateContent(
    uiState: SettingsUiState,
    createActions: (SettingsUiState.Success) -> SettingsActions
) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = uiState is SettingsUiState.Loading
    )
    var lastSuccessState by remember { mutableStateOf<SettingsUiState.Success?>(value = null) }

    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.Success) {
            lastSuccessState = uiState
        }
    }

    when (uiState) {
        is SettingsUiState.Loading -> {
            if (showLoadingIndicator) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val cachedState = lastSuccessState
                if (cachedState != null) {
                    val actions = remember(cachedState) { createActions(cachedState) }
                    SettingsContent(uiState = cachedState, actions = actions)
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        is SettingsUiState.Success -> {
            val actions = remember(uiState) { createActions(uiState) }
            SettingsContent(uiState = uiState, actions = actions)
        }
        is SettingsUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Error: ${uiState.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun createSettingsActions(
    settingsViewModel: SettingsViewModel,
    successState: SettingsUiState.Success,
    ctx: Context
): SettingsActions {
    return SettingsActions(
        onNameChange = settingsViewModel::setName,
        onEmployerChange = settingsViewModel::setEmployer,
        onWorkTypeAdded = settingsViewModel::addWorkType,
        onWorkTypeRemoved = settingsViewModel::removeWorkType,
        onSave = { settingsViewModel.saveSettings() },
        onGeneratePdf = {
            generateReport(
                ctx = ctx,
                projectsByMonth = successState.data.projectsByMonth,
                endOfMonthDate = successState.data.endMonthDate,
                name = successState.data.name,
                employer = successState.data.employer
            )
        }
    )
}

data class SettingsActions(
    val onNameChange: (String) -> Unit,
    val onEmployerChange: (String) -> Unit,
    val onWorkTypeAdded: (String) -> Unit,
    val onWorkTypeRemoved: (String) -> Unit,
    val onSave: () -> Unit,
    val onGeneratePdf: () -> Unit
)

@Composable
internal fun SettingsContent(
    uiState: SettingsUiState.Success,
    actions: SettingsActions
) {
    var showAddWorkTypeDialog by remember { mutableStateOf(value = false) }
    var selectedWorkType by remember { mutableStateOf(value = "") }

    LaunchedEffect(key1 = uiState.data.workTypes) {
        if (selectedWorkType.isEmpty() || !uiState.data.workTypes.contains(selectedWorkType)) {
            selectedWorkType = uiState.data.workTypes.firstOrNull() ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(state = rememberScrollState())
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 24.dp)
    ) {
        HeaderSection(date = uiState.data.selectedDate)

        SettingsCard(title = stringResource(id = R.string.name) + " & " + stringResource(id = R.string.employer)) {
            ProfileSection(
                name = uiState.data.name,
                employer = uiState.data.employer,
                onNameChange = actions.onNameChange,
                onEmployerChange = actions.onEmployerChange
            )
        }

        SettingsCard(title = stringResource(id = R.string.work_type)) {
            WorkTypeSection(
                workTypes = uiState.data.workTypes,
                selectedWorkType = selectedWorkType,
                onWorkTypeSelected = { selectedWorkType = it },
                onAddClick = { showAddWorkTypeDialog = true },
                onDeleteClick = {
                    actions.onWorkTypeRemoved(selectedWorkType)
                    // Reset selection will be handled by LaunchedEffect(uiState.data.workTypes)
                }
            )
        }

        ActionButtonsSection(
            onSave = actions.onSave,
            onGeneratePdf = actions.onGeneratePdf,
            isPdfEnabled = uiState.data.projectsByMonth.isNotEmpty()
        )

        if (showAddWorkTypeDialog) {
            AddTextFieldDialog(
                onDismissRequest = { showAddWorkTypeDialog = false },
                onConfirmation = {
                    actions.onWorkTypeAdded(it)
                    showAddWorkTypeDialog = false
                },
                label = stringResource(id = R.string.work_type)
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
    ) {
        Column(modifier = Modifier.padding(all = 16.dp), verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun HeaderSection(date: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            Header(title = stringResource(id = R.string.settings))
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ProfileSection(
    name: String,
    employer: String,
    onNameChange: (String) -> Unit,
    onEmployerChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        SettingsTextField(value = name, label = R.string.name, onValueChange = onNameChange)
        SettingsTextField(value = employer, label = R.string.employer, onValueChange = onEmployerChange)
    }
}

@Composable
private fun SettingsTextField(value: String, label: Int, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(text = stringResource(id = label)) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun WorkTypeSection(
    workTypes: List<String>,
    selectedWorkType: String,
    onWorkTypeSelected: (String) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        DropdownMenuBox(
            items = workTypes,
            onItemSelected = onWorkTypeSelected,
            selectedText = selectedWorkType,
            labelId = R.string.work_type,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            Button(
                onClick = onAddClick,
                modifier = Modifier.weight(weight = 1f),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.add))
            }
            Button(
                onClick = onDeleteClick,
                enabled = selectedWorkType.isNotEmpty(),
                modifier = Modifier.weight(weight = 1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.delete))
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onSave: () -> Unit,
    onGeneratePdf: () -> Unit,
    isPdfEnabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.save), fontSize = 18.sp)
        }
        Button(
            onClick = onGeneratePdf,
            enabled = isPdfEnabled,
            modifier = Modifier.fillMaxWidth(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.generate_pdf), fontSize = 18.sp)
        }
        Text(
            text = stringResource(id = R.string.monthly_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
        )
    }
}

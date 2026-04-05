package com.akiwiksten.worktime30.feature.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.text.isDigitsOnly
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.TIME_FORMAT
import com.akiwiksten.worktime30.core.WorkTimeCalculator
import com.akiwiksten.worktime30.core.ui.DropdownMenuBox
import com.akiwiksten.worktime30.core.ui.MyAlertDialog
import com.akiwiksten.worktime30.core.ui.TimePickerDialog
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun ProjectDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (uiState: ProjectListItemUiState) -> Unit,
    uiState: ProjectListItemUiState,
    workTypeDropDownList: List<String>
) {
    var state by remember { mutableStateOf(ProjectDialogState(uiState)) }
    var isNegativeWorkDay by remember { mutableStateOf(false) }

    val isConfirmEnabled by remember {
        derivedStateOf {
            state.projectName.isNotBlank() && state.kilometres.isDigitsOnly()
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(uiState.titleId),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                DialogMainFields(
                    state = state,
                    isAddMode = uiState.titleId == R.string.add,
                    onStateChange = { state = it }
                )

                TimeSelectionSection(
                    state = state,
                    onStateChange = { state = it },
                    onNegativeTime = { isNegativeWorkDay = true }
                )

                DialogDropdownFields(
                    state = state,
                    workTypeDropDownList = workTypeDropDownList,
                    onStateChange = { state = it }
                )

                DialogActionButtons(
                    onDismiss = onDismissRequest,
                    onConfirm = { onConfirmation(state.toUiState()) },
                    confirmEnabled = isConfirmEnabled
                )
            }
        }
    }

    if (isNegativeWorkDay) {
        NegativeWorkTimeAlert(onDismiss = { isNegativeWorkDay = false })
    }
}

@Composable
private fun DialogMainFields(
    state: ProjectDialogState,
    isAddMode: Boolean,
    onStateChange: (ProjectDialogState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = state.projectName,
            onValueChange = { onStateChange(state.copy(projectName = it)) },
            label = { Text(stringResource(R.string.project_name)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = isAddMode,
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        OutlinedTextField(
            value = state.kilometres,
            onValueChange = { if (it.isDigitsOnly()) onStateChange(state.copy(kilometres = it)) },
            label = { Text(stringResource(R.string.kilometres)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun TimeSelectionSection(
    state: ProjectDialogState,
    onStateChange: (ProjectDialogState) -> Unit,
    onNegativeTime: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        CompactTimeRow(
            labelId = R.string.start_time,
            value = state.projectStartTime,
            onTimeChanged = { 
                onStateChange(state.copy(projectStartTime = it, projectEndTime = it)) 
            }
        )
        CompactTimeRow(
            labelId = R.string.end_time,
            value = state.projectEndTime,
            onTimeChanged = {
                onStateChange(state.copy(projectEndTime = it))
                if (WorkTimeCalculator.calculateWorkTimeBalance(it, "-${state.projectStartTime}")
                        .startsWith('-')
                ) onNegativeTime()
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
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DropdownMenuBox(
            items = listOf(
                stringResource(R.string.no_allowance),
                stringResource(R.string.daily_allowance),
                stringResource(R.string.half_day_allowance)
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
private fun CompactTimeRow(labelId: Int, value: String, onTimeChanged: (String) -> Unit) {
    var openPicker by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(stringResource(labelId)) },
            readOnly = true,
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline
            ),
            enabled = false,
            shape = RoundedCornerShape(12.dp)
        )
        
        IconButton(
            onClick = {
                onTimeChanged(LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT)))
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        
        IconButton(
            onClick = { openPicker = true },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }

    if (openPicker) {
        TimePickerDialog(
            onDismissRequest = { openPicker = false },
            onConfirmation = {
                onTimeChanged(it)
                openPicker = false
            },
            time = value,
            titleId = labelId
        )
    }
}

@Composable
private fun DialogActionButtons(onDismiss: () -> Unit, onConfirm: () -> Unit, confirmEnabled: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.dismiss))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = onConfirm,
            enabled = confirmEnabled,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.confirm))
        }
    }
}

@Composable
private fun NegativeWorkTimeAlert(onDismiss: () -> Unit) {
    MyAlertDialog(
        onDismissRequest = onDismiss,
        onConfirmation = onDismiss,
        dialogTitle = stringResource(R.string.negative_worktime_title),
        dialogText = stringResource(R.string.negative_worktime_text),
        icon = Icons.Default.Info
    )
}

private data class ProjectDialogState(
    val projectName: String,
    val projectStartTime: String,
    val projectEndTime: String,
    val kilometres: String,
    val allowance: String,
    val workType: String
) {
    constructor(uiState: ProjectListItemUiState) : this(
        projectName = uiState.projectName,
        projectStartTime = uiState.projectStartTime,
        projectEndTime = uiState.projectEndTime,
        kilometres = uiState.kilometres.toString(),
        allowance = uiState.allowance.ifEmpty { "No Allowance" },
        workType = uiState.workType
    )

    fun toUiState() = ProjectListItemUiState(
        projectName = projectName,
        projectStartTime = projectStartTime,
        projectEndTime = projectEndTime,
        kilometres = kilometres.toIntOrNull() ?: 0,
        allowance = allowance,
        workType = workType
    )
}

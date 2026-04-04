package com.akiwiksten.worktime30.feature.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(uiState.titleId),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                ProjectNameField(
                    name = state.projectName,
                    enabled = uiState.titleId == R.string.add,
                    onNameChange = { state = state.copy(projectName = it) }
                )

                ProjectTimeSection(
                    startTime = state.projectStartTime,
                    endTime = state.projectEndTime,
                    onStartTimeChanged = { state = state.copy(projectStartTime = it, projectEndTime = it) },
                    onEndTimeChanged = {
                        state = state.copy(projectEndTime = it)
                        if (WorkTimeCalculator.calculateWorkTimeBalance(it, "-${state.projectStartTime}")
                                .startsWith('-')
                        ) isNegativeWorkDay = true
                    }
                )

                ProjectDropdowns(
                    state = state,
                    workTypeDropDownList = workTypeDropDownList,
                    onAllowanceSelected = { state = state.copy(allowance = it) },
                    onWorkTypeSelected = { state = state.copy(workType = it) }
                )

                KilometresField(
                    value = state.kilometres,
                    onValueChange = { if (it.isDigitsOnly()) state = state.copy(kilometres = it) }
                )

                DialogActionButtons(
                    onDismiss = onDismissRequest,
                    onConfirm = { onConfirmation(state.toUiState()) },
                    confirmEnabled = state.projectName.isNotEmpty() && state.kilometres.isDigitsOnly()
                )
            }
        }
    }

    if (isNegativeWorkDay) {
        NegativeWorkTimeAlert(onDismiss = { isNegativeWorkDay = false })
    }
}

@Composable
private fun ProjectNameField(name: String, enabled: Boolean, onNameChange: (String) -> Unit) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.project_name)) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
        isError = name.isEmpty(),
        enabled = enabled
    )
}

@Composable
private fun KilometresField(value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.kilometres)) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold),
        isError = !value.isDigitsOnly(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
private fun ProjectDropdowns(
    state: ProjectDialogState,
    workTypeDropDownList: List<String>,
    onAllowanceSelected: (String) -> Unit,
    onWorkTypeSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DropdownMenuBox(
            items = listOf(
                stringResource(R.string.no_allowance),
                stringResource(R.string.daily_allowance),
                stringResource(R.string.half_day_allowance)
            ),
            onItemSelected = onAllowanceSelected,
            labelId = R.string.allowance,
            selectedText = state.allowance,
            modifier = Modifier.fillMaxWidth()
        )

        DropdownMenuBox(
            items = workTypeDropDownList,
            onItemSelected = onWorkTypeSelected,
            labelId = R.string.work_type,
            selectedText = state.workType,
            modifier = Modifier.fillMaxWidth()
        )
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

@Composable
private fun ProjectTimeSection(
    startTime: String,
    endTime: String,
    onStartTimeChanged: (String) -> Unit,
    onEndTimeChanged: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CompactTimeRow(stringResource(R.string.start_time), startTime, onStartTimeChanged)
        CompactTimeRow(stringResource(R.string.end_time), endTime, onEndTimeChanged)
    }
}

@Composable
private fun CompactTimeRow(label: String, value: String, onTimeChanged: (String) -> Unit) {
    var openPicker by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            modifier = Modifier.weight(1f),
            textStyle = TextStyle(fontWeight = FontWeight.Bold)
        )
        Button(onClick = {
            onTimeChanged(LocalDateTime.now().format(DateTimeFormatter.ofPattern(TIME_FORMAT)))
        }) {
            Text(stringResource(R.string.current_time), fontSize = 12.sp)
        }
        Button(onClick = { openPicker = true }) {
            Text(stringResource(R.string.go_to_time_picker), fontSize = 12.sp)
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
            titleId = R.string.select_date
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
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.dismiss)) }
        Spacer(modifier = Modifier.width(8.dp))
        Button(onClick = onConfirm, enabled = confirmEnabled) { Text(stringResource(R.string.confirm)) }
    }
}

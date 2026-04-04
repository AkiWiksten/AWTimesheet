package com.akiwiksten.worktime30.feature.projects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
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
import com.akiwiksten.worktime30.core.TimeGeneratorModel
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.ui.DropdownMenuBox
import com.akiwiksten.worktime30.core.ui.MyAlertDialog
import com.akiwiksten.worktime30.core.ui.TimePickerDialog
import com.akiwiksten.worktime30.feature.editworkday.AddTimeRow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
@Suppress("FunctionNaming", "LongMethod")
fun ProjectDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (uiState: ProjectListItemUiState) -> Unit,
    uiState: ProjectListItemUiState,
    workTypeDropDownList: List<String>
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        val noAllowance = stringResource(R.string.no_allowance)
        var projectName by remember { mutableStateOf(uiState.projectName) }
        var projectStartTime by remember { mutableStateOf(uiState.projectStartTime) }
        var projectEndTime by remember { mutableStateOf(uiState.projectEndTime) }
        var kilometres by remember { mutableStateOf(uiState.kilometres.toString()) }
        var allowance by remember { mutableStateOf(uiState.allowance.ifEmpty { noAllowance }) }
        var workType by remember { mutableStateOf(uiState.workType) }
        var isNegativeWorkDay by remember { mutableStateOf(false) }

        Card(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .height(IntrinsicSize.Min)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(10.dp).fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = stringResource(uiState.titleId), fontSize = 20.sp)
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text(stringResource(R.string.project_name), fontSize = 20.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    textStyle = TextStyle.Default.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    isError = projectName.isEmpty(),
                    enabled = uiState.titleId == R.string.add
                )
                AddTimeRow(
                    textFieldValue = projectStartTime,
                    stringId = R.string.start_time,
                    currentTime = fun() {
                        val formatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
                        projectStartTime = LocalDateTime.now().format(formatter)
                        projectEndTime = projectStartTime
                    },
                    onConfirmation = fun(time) {
                        projectStartTime = time
                        projectEndTime = time
                    }
                )
                AddTimeRow(
                    textFieldValue = projectEndTime,
                    stringId = R.string.end_time,
                    currentTime = fun() {
                        val formatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
                        projectEndTime = LocalDateTime.now().format(formatter)
                        val timeDifference = TimeGeneratorModel.calculateWorkTimeBalance(projectEndTime,
                            "-$projectStartTime"
                        )
                        if (timeDifference.startsWith('-')) {
                            isNegativeWorkDay = true
                        }
                    },
                    onConfirmation = fun(time) {
                        projectEndTime = time
                        val timeDifference = TimeGeneratorModel.calculateWorkTimeBalance(projectEndTime,
                            "-$projectStartTime"
                        )
                        if (timeDifference.startsWith('-')) {
                            isNegativeWorkDay = true
                        }
                    }
                )
                DropdownMenuBox(
                    items = listOf(
                        stringResource(R.string.no_allowance),
                        stringResource(R.string.daily_allowance),
                        stringResource(R.string.half_day_allowance)
                    ),
                    onItemSelected = { allowance = it },
                    labelId = R.string.allowance,
                    selectedText = allowance
                )
                DropdownMenuBox(
                    items = workTypeDropDownList,
                    onItemSelected = { workType = it },
                    labelId = R.string.work_type,
                    selectedText = workType
                )
                OutlinedTextField(
                    value = kilometres,
                    onValueChange = { kilometres = it },
                    label = { Text(stringResource(R.string.kilometres), fontSize = 20.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    textStyle = TextStyle.Default.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    isError = !kilometres.isDigitsOnly(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(
                        onClick = { onDismissRequest() },
                    ) {
                        Text(stringResource(R.string.dismiss))
                    }
                    TextButton(
                        onClick = {
                            onConfirmation(
                                ProjectListItemUiState(
                                    projectName = projectName,
                                    projectStartTime = projectStartTime,
                                    projectEndTime = projectEndTime,
                                    kilometres = kilometres.toInt(),
                                    allowance = allowance,
                                    workType = workType,
                                )
                            )
                        },
                        enabled = projectName.isNotEmpty() &&
                                kilometres.isDigitsOnly()
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
                Text(stringResource(R.string.work_type_help))
            }
            if(isNegativeWorkDay) {
                MyAlertDialog(
                    onDismissRequest = { isNegativeWorkDay = false },
                    onConfirmation = {
                        isNegativeWorkDay = false
                    },
                    dialogTitle = stringResource(R.string.negative_worktime_title),
                    dialogText = stringResource(R.string.negative_worktime_text),
                    icon = Icons.Default.Info
                )
            }
        }
    }
}

@Composable
@Suppress("FunctionNaming")
fun AddTimeRow(
    textFieldValue: MutableState<String>,
    stringId: Int
) {
    var openTimePickerDialog by remember { mutableStateOf(false) }
    Row(modifier = Modifier.padding(5.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = textFieldValue.value,
            onValueChange = {},
            label = { Text(stringResource(stringId), fontSize = 20.sp) },
            enabled = false,
            textStyle = TextStyle.Default.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier
                .width(100.dp)
                .padding(5.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { textFieldValue.value = ZERO_TIME }
            )
            {
                Text(stringResource(R.string.reset), fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = { openTimePickerDialog = true }
            )
            {
                Text(stringResource(R.string.go_to_time_picker), fontSize = 15.sp)
            }
        }
        if(openTimePickerDialog) {
            TimePickerDialog(
                onDismissRequest = { openTimePickerDialog = false },
                onConfirmation = fun(time) {
                    textFieldValue.value = time
                    openTimePickerDialog = false
                    println("Time received") // Add logic here to handle confirmation.
                },
                time = textFieldValue.value,
                titleId = stringId
            )
        }
    }
}

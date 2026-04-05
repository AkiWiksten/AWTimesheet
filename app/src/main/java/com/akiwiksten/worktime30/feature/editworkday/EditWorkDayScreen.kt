package com.akiwiksten.worktime30.feature.editworkday

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.TimePickerDialog
import com.akiwiksten.worktime30.feature.calendar.CalendarViewModel

@Composable
fun EditWorkDayScreen(
    onItemClick: () -> Unit,
    calendarViewModel: CalendarViewModel = hiltViewModel(),
    viewModel: EditWorkDayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.setDate(date0 = calendarViewModel.uiState.value.date)
        viewModel.loadWorkDay()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection(date = uiState.date, onClearDay = viewModel::clearDay)

        if (uiState.isNewDay) {
            NewDayFields(uiState = uiState, viewModel = viewModel)
        } else {
            ExistingDayFields(uiState = uiState, viewModel = viewModel)
        }

        FooterSection(
            onSave = {
                viewModel.insertWorkDay()
                onItemClick()
            }
        )
    }
}

@Composable
private fun HeaderSection(date: String, onClearDay: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Header(stringResource(R.string.work_day))
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = onClearDay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(stringResource(R.string.clear_day))
            }
        }
    }
}

@Composable
private fun NewDayFields(uiState: EditWorkDayUiState, viewModel: EditWorkDayViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AddTimeRow(uiState.startTime, R.string.start_time, viewModel::currentStartTime, viewModel::setStartTime)
        AddTimeRow(uiState.dailyWorkTime, R.string.daily_work_time, viewModel::currentDailyWorkTime, viewModel::setDailyWorkTime)
        AddTimeRow(uiState.lunchTime, R.string.lunch_time, viewModel::currentLunchTime, viewModel::setLunchTime)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                AddCustomTimeRow(uiState.balanceTotal, viewModel::setBalanceTotal, R.string.balance_total)
            }
            Box(modifier = Modifier.weight(1f)) {
                AddCustomTimeRow(uiState.workTimeTotal, viewModel::setWorkTimeTotal, R.string.work_time_total)
            }
        }
        Text(
            text = stringResource(R.string.new_day),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
private fun ExistingDayFields(uiState: EditWorkDayUiState, viewModel: EditWorkDayViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AddTimeRow(uiState.startTime, R.string.start_time, viewModel::currentStartTime, viewModel::setStartTime)
        AddTimeRow(uiState.endTime, R.string.end_time, viewModel::currentEndTime, viewModel::setEndTime)
        AddTimeRow(uiState.workTimeToday, R.string.work_time_today, viewModel::currentWorkTimeToday, viewModel::setWorkTimeToday)
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        
        AddTimeRow(uiState.lunchStart, R.string.lunch_start, viewModel::currentLunchStart, viewModel::setLunchStart)
        AddTimeRow(uiState.lunchEnd, R.string.lunch_end, viewModel::currentLunchEnd, viewModel::setLunchEnd)
        AddTimeRow(uiState.breakStart, R.string.break_start, viewModel::currentBreakStart, viewModel::setBreakStart)
        AddTimeRow(uiState.breakEnd, R.string.break_end, viewModel::currentBreakEnd, viewModel::setBreakEnd)
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        
        AddTimeRow(uiState.dailyWorkTime, R.string.daily_work_time, viewModel::currentDailyWorkTime, viewModel::setDailyWorkTime)
        AddTimeRow(uiState.lunchTime, R.string.lunch_time, viewModel::currentLunchTime, viewModel::setLunchTime)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    AddCustomTimeRow(uiState.balanceToday, viewModel::setBalanceToday, R.string.balance_today)
                }
                Box(modifier = Modifier.weight(1f)) {
                    AddCustomTimeRow(uiState.balanceTotal, viewModel::setBalanceTotal, R.string.balance_total)
                }
            }
            AddCustomTimeRow(uiState.workTimeTotal, viewModel::setWorkTimeTotal, R.string.work_time_total)
        }
    }
}

@Composable
private fun FooterSection(onSave: () -> Unit) {
    val ctx = LocalContext.current
    val saveString = stringResource(R.string.saved)

    Button(
        onClick = {
            onSave()
            Toast.makeText(ctx, saveString, Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Text(stringResource(R.string.save), style = MaterialTheme.typography.titleLarge)
    }
}

private fun isValidText(text: String): Boolean {
    return text.matches(Regex("-?[1-9][0-9]+:[0-5][0-9]")) ||
        text.matches(Regex("-?0[0-9]:[0-5][0-9]"))
}

@Composable
private fun AddCustomTimeRow(
    customTime: String,
    customTimeFunction: (String, Boolean) -> Unit,
    stringId: Int
) {
    OutlinedTextField(
        value = customTime,
        onValueChange = { customTimeFunction(it, isValidText(it)) },
        singleLine = true,
        label = { Text(stringResource(stringId)) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        isError = !isValidText(customTime)
    )
}

@Composable
private fun AddTimeRow(
    textFieldValue: String,
    stringId: Int,
    currentTime: () -> Unit,
    onConfirmation: (time: String) -> Unit,
) {
    var openTimePickerDialog by remember { mutableStateOf(false) }

    if (openTimePickerDialog) {
        TimePickerDialog(
            onDismissRequest = { openTimePickerDialog = false },
            onConfirmation = { time ->
                onConfirmation(time)
                openTimePickerDialog = false
            },
            time = textFieldValue,
            titleId = stringId,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {},
            label = { Text(stringResource(stringId)) },
            readOnly = true,
            enabled = false,
            isError = !isValidText(textFieldValue),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
            )
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
                onClick = currentTime,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = stringResource(R.string.current_time),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = { openTimePickerDialog = true },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = stringResource(R.string.go_to_time_picker),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

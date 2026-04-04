package com.akiwiksten.worktime30.feature.editworkday

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.TimePickerDialog
import com.akiwiksten.worktime30.feature.calendar.CalendarViewModel

@Composable
fun EditWorkDayScreen(
    onItemClick: () -> Unit,
    calendarViewModel: CalendarViewModel = hiltViewModel(),
    editWorkDayViewModel: EditWorkDayViewModel = hiltViewModel(),
) {
    val date by editWorkDayViewModel.date.collectAsState()
    val isNewDay by editWorkDayViewModel.isNewDay.collectAsState()

    LaunchedEffect(Unit) {
        editWorkDayViewModel.setDate(date0 = calendarViewModel.uiState.value.date)
        editWorkDayViewModel.loadWorkDay()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection(date = date, onClearDay = editWorkDayViewModel::clearDay)

        if (isNewDay) {
            NewDayFields(viewModel = editWorkDayViewModel)
        } else {
            ExistingDayFields(viewModel = editWorkDayViewModel)
        }

        FooterSection(
            onSave = {
                editWorkDayViewModel.insertWorkDay()
                onItemClick()
            }
        )
    }
}

@Composable
private fun HeaderSection(date: String, onClearDay: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Header(stringResource(R.string.work_day))
        Text(
            text = date,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Button(onClick = onClearDay) {
            Text(stringResource(R.string.clear_day))
        }
    }
}

@Composable
private fun NewDayFields(viewModel: EditWorkDayViewModel) {
    val startTime by viewModel.startTime.collectAsState()
    val dailyWorkTime by viewModel.dailyWorkTime.collectAsState()
    val lunchTime by viewModel.lunchTime.collectAsState()
    val balanceTotal by viewModel.balanceTotal.collectAsState()
    val workTimeTotal by viewModel.workTimeTotal.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AddTimeRow(startTime, R.string.start_time, viewModel::currentStartTime, viewModel::setStartTime)
        AddTimeRow(dailyWorkTime, R.string.daily_work_time, viewModel::currentDailyWorkTime, viewModel::setDailyWorkTime)
        AddTimeRow(lunchTime, R.string.lunch_time, viewModel::currentLunchTime, viewModel::setLunchTime)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            AddCustomTimeRow(balanceTotal, viewModel::setBalanceTotal, R.string.balance_total)
            Spacer(modifier = Modifier.width(8.dp))
            AddCustomTimeRow(workTimeTotal, viewModel::setWorkTimeTotal, R.string.work_time_total)
        }
        Text(
            text = stringResource(R.string.new_day),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun ExistingDayFields(viewModel: EditWorkDayViewModel) {
    val startTime by viewModel.startTime.collectAsState()
    val endTime by viewModel.endTime.collectAsState()
    val workTimeToday by viewModel.workTimeToday.collectAsState()
    val lunchStart by viewModel.lunchStart.collectAsState()
    val lunchEnd by viewModel.lunchEnd.collectAsState()
    val breakStart by viewModel.breakStart.collectAsState()
    val breakEnd by viewModel.breakEnd.collectAsState()
    val dailyWorkTime by viewModel.dailyWorkTime.collectAsState()
    val lunchTime by viewModel.lunchTime.collectAsState()
    val balanceToday by viewModel.balanceToday.collectAsState()
    val balanceTotal by viewModel.balanceTotal.collectAsState()
    val workTimeTotal by viewModel.workTimeTotal.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AddTimeRow(startTime, R.string.start_time, viewModel::currentStartTime, viewModel::setStartTime)
        AddTimeRow(endTime, R.string.end_time, viewModel::currentEndTime, viewModel::setEndTime)
        AddTimeRow(workTimeToday, R.string.work_time_today, viewModel::currentWorkTimeToday, viewModel::setWorkTimeToday)
        AddTimeRow(lunchStart, R.string.lunch_start, viewModel::currentLunchStart, viewModel::setLunchStart)
        AddTimeRow(lunchEnd, R.string.lunch_end, viewModel::currentLunchEnd, viewModel::setLunchEnd)
        AddTimeRow(breakStart, R.string.break_start, viewModel::currentBreakStart, viewModel::setBreakStart)
        AddTimeRow(breakEnd, R.string.break_end, viewModel::currentBreakEnd, viewModel::setBreakEnd)
        AddTimeRow(dailyWorkTime, R.string.daily_work_time, viewModel::currentDailyWorkTime, viewModel::setDailyWorkTime)
        AddTimeRow(lunchTime, R.string.lunch_time, viewModel::currentLunchTime, viewModel::setLunchTime)

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.Center) {
                AddCustomTimeRow(balanceToday, viewModel::setBalanceToday, R.string.balance_today)
                Spacer(modifier = Modifier.width(8.dp))
                AddCustomTimeRow(balanceTotal, viewModel::setBalanceTotal, R.string.balance_total)
            }
            AddCustomTimeRow(workTimeTotal, viewModel::setWorkTimeTotal, R.string.work_time_total)
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
        modifier = Modifier.padding(top = 16.dp)
    ) {
        Text(stringResource(R.string.save), fontSize = 20.sp)
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
        label = { Text(stringResource(stringId), fontSize = 16.sp) },
        modifier = Modifier.width(130.dp),
        textStyle = TextStyle.Default.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
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
        horizontalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {},
            label = { Text(stringResource(stringId), fontSize = 16.sp) },
            readOnly = true,
            enabled = false,
            isError = !isValidText(textFieldValue),
            textStyle = TextStyle.Default.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.width(110.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Button(onClick = currentTime, modifier = Modifier.width(140.dp)) {
                Text(stringResource(R.string.current_time), fontSize = 14.sp)
            }
            Button(onClick = { openTimePickerDialog = true }, modifier = Modifier.width(140.dp)) {
                Text(stringResource(R.string.go_to_time_picker), fontSize = 14.sp)
            }
        }
    }
}

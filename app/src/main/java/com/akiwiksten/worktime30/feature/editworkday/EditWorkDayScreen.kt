package com.akiwiksten.worktime30.feature.editworkday

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.TimePickerDialog
import com.akiwiksten.worktime30.feature.calendar.CalendarViewModel
import com.akiwiksten.worktime30.feature.projects.ProjectsViewModel
import com.akiwiksten.worktime30.feature.settings.SettingsViewModel

@Composable
@Suppress("LongMethod", "FunctionNaming", "CyclomaticComplexMethod")
fun EditWorkDayScreen(
    onItemClick: () -> Unit,
    calendarViewModel: CalendarViewModel = hiltViewModel(),
    editWorkDayViewModel: EditWorkDayViewModel = hiltViewModel(),
) {
    val date by editWorkDayViewModel.date.collectAsState()
    val startTime by editWorkDayViewModel.startTime.collectAsState()
    val endTime by editWorkDayViewModel.endTime.collectAsState()
    val dailyWorkTime by editWorkDayViewModel.dailyWorkTime.collectAsState()
    val lunchStart by editWorkDayViewModel.lunchStart.collectAsState()
    val lunchEnd by editWorkDayViewModel.lunchEnd.collectAsState()
    val lunchTime by editWorkDayViewModel.lunchTime.collectAsState()
    val breakStart by editWorkDayViewModel.breakStart.collectAsState()
    val breakEnd by editWorkDayViewModel.breakEnd.collectAsState()
    val workTimeToday by editWorkDayViewModel.workTimeToday.collectAsState()
    val workTimeTotal by editWorkDayViewModel.workTimeTotal.collectAsState()
    val balanceToday by editWorkDayViewModel.balanceToday.collectAsState()
    val balanceTotal by editWorkDayViewModel.balanceTotal.collectAsState()
    val isNewDay by editWorkDayViewModel.isNewDay.collectAsState()
    val recompose = remember { mutableStateOf(true) }
    val ctx = LocalContext.current
    editWorkDayViewModel.setCtx(ctx)

    LaunchedEffect(Unit) {
        editWorkDayViewModel.setDate(date0 = calendarViewModel.date.value)
        editWorkDayViewModel.loadWorkDay()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val saveString = stringResource(R.string.saved)

        Header(stringResource(R.string.work_day))
        Row(modifier = Modifier.padding(16.dp)) {
            Text(
                text = date,
                fontSize = 30.sp,
            )
        }
        Button(
            onClick = {
                editWorkDayViewModel.clearDay()
            }
        ) {
            Text(stringResource(R.string.clear_day), fontSize = 20.sp)
        }
        AddTimeRow(
            textFieldValue = startTime,
            stringId = R.string.start_time,
            currentTime = fun () {
                editWorkDayViewModel.currentStartTime()
            },
            onConfirmation = fun (time) {
                editWorkDayViewModel.setStartTime(startTime0 = time)
            }
        )
        if(isNewDay) {
            AddTimeRow(
                textFieldValue = dailyWorkTime,
                stringId = R.string.daily_work_time,
                currentTime = fun () {
                    editWorkDayViewModel.currentDailyWorkTime()
                },
                onConfirmation = fun (time) {
                    editWorkDayViewModel.setDailyWorkTime(dailyWorkTime0 = time)
                }
            )
            AddTimeRow(
                textFieldValue = lunchTime,
                stringId = R.string.lunch_time,
                currentTime = fun () {
                    editWorkDayViewModel.currentLunchTime()
                },
                onConfirmation = fun (time) {
                    editWorkDayViewModel.setLunchTime(lunchTime0 = time)
                }
            )
            Row(modifier = Modifier.padding(16.dp)) {
                AddCustomTimeRow(
                    customTime = balanceTotal,
                    customTimeFunction = fun (value, isValid) {
                        editWorkDayViewModel.setBalanceTotal(value, isValid)
                    },
                    stringId = R.string.balance_total
                )
                AddCustomTimeRow(
                    customTime = workTimeTotal,
                    customTimeFunction = fun (value, isValid) {
                        editWorkDayViewModel.setWorkTimeTotal(value, isValid)
                    },
                    stringId = R.string.work_time_total
                )
            }
            Text(stringResource(R.string.new_day), fontSize = 15.sp)
        } else {
            AddTimeRow(
                textFieldValue = endTime,
                stringId = R.string.end_time,
                currentTime = fun () {
                    editWorkDayViewModel.currentEndTime()
                },
                onConfirmation = fun (time) {
                    editWorkDayViewModel.setEndTime(endTime0 = time)
                }
            )
            AddTimeRow(
                textFieldValue = workTimeToday,
                stringId = R.string.work_time_today,
                currentTime = fun () {
                    editWorkDayViewModel.currentWorkTimeToday()
                },
                onConfirmation = fun (time) {
                    editWorkDayViewModel.setWorkTimeToday(workTimeToday0 = time)
                }
            )
            AddTimeRow(
                textFieldValue = lunchStart,
                stringId = R.string.lunch_start,
                currentTime = fun () {
                    editWorkDayViewModel.currentLunchStart()
                },
                onConfirmation = fun (time) {
                    editWorkDayViewModel.setLunchStart(lunchStart0 = time)
                }
            )
            AddTimeRow(
                textFieldValue = lunchEnd,
                stringId = R.string.lunch_end,
                currentTime = fun () {
                    editWorkDayViewModel.currentLunchEnd()
                },
                onConfirmation = fun (time) {
                    editWorkDayViewModel.setLunchEnd(lunchEnd0 = time)
                }
            )
            AddTimeRow(
                textFieldValue = breakStart,
                stringId = R.string.break_start,
                currentTime = fun () {
                    editWorkDayViewModel.currentBreakStart()
                },
                onConfirmation = fun (time) {
                    editWorkDayViewModel.setBreakStart(breakStart0 = time)
                }
            )
            AddTimeRow(
                textFieldValue = breakEnd,
                stringId = R.string.break_end,
                currentTime = fun () {
                    editWorkDayViewModel.currentBreakEnd()
                },
                onConfirmation = fun (time) {
                    editWorkDayViewModel.setBreakEnd(breakEnd0 = time)
                }
            )
            AddTimeRow(
                textFieldValue = dailyWorkTime,
                stringId = R.string.daily_work_time,
                currentTime = fun () {
                    editWorkDayViewModel.currentDailyWorkTime()
                },
                onConfirmation = fun (time) {
                    editWorkDayViewModel.setDailyWorkTime(dailyWorkTime0 = time)
                }
            )
            AddTimeRow(
                textFieldValue = lunchTime,
                stringId = R.string.lunch_time,
                currentTime = fun () {
                    editWorkDayViewModel.currentLunchTime()
                },
                onConfirmation = fun (time0) {
                    editWorkDayViewModel.setLunchTime(time0)
                }
            )
            Row(modifier = Modifier.padding(16.dp)) {
                AddCustomTimeRow(
                    customTime = balanceToday,
                    customTimeFunction = fun (value, isValid) {
                        editWorkDayViewModel.setBalanceToday(value, isValid)
                    },
                    stringId = R.string.balance_today
                )
                AddCustomTimeRow(
                    customTime = balanceTotal,
                    customTimeFunction = fun (value, isValid) {
                        editWorkDayViewModel.setBalanceTotal(value, isValid)
                    },
                    stringId = R.string.balance_total
                )
                AddCustomTimeRow(
                    customTime = workTimeTotal,
                    customTimeFunction = fun (value, isValid) {
                        editWorkDayViewModel.setWorkTimeTotal(value, isValid)
                    },
                    stringId = R.string.work_time_total
                )
            }
        }
        Row(modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {

            Button(
                onClick = {
                    editWorkDayViewModel.insertWorkDay()
                    onItemClick()
                    Toast.makeText(ctx, saveString, Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .padding(5.dp)
            )
            {
                Text(stringResource(R.string.save), fontSize = 20.sp)
            }
        }
    }
}

fun isValidText(text : String) : Boolean {
    return text.matches(Regex("-?[1-9][0-9]+:[0-5][0-9]")) ||
            text.matches(Regex("-?0[0-9]:[0-5][0-9]"))
}

@Composable
@Suppress("FunctionNaming")
fun AddCustomTimeRow(customTime: String, customTimeFunction: (String, Boolean) -> Unit, stringId: Int) {
    OutlinedTextField(
        value = customTime,
        onValueChange = { customTimeFunction(it, isValidText(it)) },
        singleLine = true,
        label = { Text(stringResource(stringId), fontSize = 20.sp) },
        modifier = Modifier
            .width(120.dp)
            .padding(5.dp),
        textStyle = TextStyle.Default.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
        isError = !isValidText(customTime)
    )
}

@Composable
@Suppress("FunctionNaming")
fun AddTimeRow(
    textFieldValue: String,
    stringId: Int,
    currentTime: () -> Unit,
    onConfirmation: (time: String) -> Unit,
) {

    var openTimePickerDialog by remember { mutableStateOf(false) }
    var isOpen by remember { mutableStateOf(false) }

    if(openTimePickerDialog) {
        isOpen = false
        TimePickerDialog(
            onDismissRequest = { openTimePickerDialog = false },
            onConfirmation = fun (time) {
                openTimePickerDialog = false
                onConfirmation(time)
            },

            time = textFieldValue,
            titleId = stringId,
        )

    }
    Row(
        modifier = Modifier.padding(5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {},
            label = { Text(stringResource(stringId), fontSize = 20.sp) },
            enabled = false,
            isError = !isValidText(textFieldValue),
            textStyle = TextStyle.Default.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier
                .width(100.dp)
                .padding(5.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Button(
                onClick = {
                    currentTime()
                },
                modifier = Modifier.width(100.dp)
            )
            {
                Text(stringResource(R.string.current_time), fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = { openTimePickerDialog = true },
                modifier = Modifier.width(100.dp)
            )
            {
                Text(stringResource(R.string.go_to_time_picker), fontSize = 15.sp)
            }
        }
    }
}

package com.akiwiksten.worktime30.feature.editworkday

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.TimePickerDialog
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkDayOneRowEntity
import com.akiwiksten.worktime30.feature.calendar.CalendarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkDayScreen(
    projectName: String? = null,
    workDay: WorkDayEntity? = null,
    workDayOneRow: WorkDayOneRowEntity? = null,
    onNavigateBack: () -> Unit,
    onConfirm: (WorkDayEntity, WorkDayOneRowEntity) -> Unit,
    calendarViewModel: CalendarViewModel = hiltViewModel(),
    viewModel: EditWorkDayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler { onNavigateBack() }

    LaunchedEffect(Unit) {
        viewModel.setDate(date0 = calendarViewModel.uiState.value.date)
        projectName?.let { viewModel.setProjectName(it) }
        viewModel.loadWorkDay(workDay, workDayOneRow)
    }

    Scaffold(
        topBar = {
            EditWorkDayTopBar(onNavigateBack = onNavigateBack)
        }
    ) { padding ->
        EditWorkDayContent(
            padding = padding,
            uiState = uiState,
            projectName = projectName,
            viewModel = viewModel,
            onConfirm = {
                val workDayResult = viewModel.getWorkDayEntity()
                val workDayOneRowResult = viewModel.getWorkDayOneRowEntity()
                onConfirm(workDayResult, workDayOneRowResult)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditWorkDayTopBar(onNavigateBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Header(
                title = stringResource(R.string.work_day),
                modifier = Modifier.padding(top = 0.dp),
                fillMaxWidth = false
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }
    )
}

@Composable
private fun EditWorkDayContent(
    padding: PaddingValues,
    uiState: EditWorkDayUiState,
    projectName: String?,
    viewModel: EditWorkDayViewModel,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection(date = uiState.date, onClearDay = viewModel::clearDay)

        projectName?.let {
            ProjectNameField(name = it)
        }

        if (uiState.isNewDay) {
            NewDayFields(uiState = uiState, viewModel = viewModel)
        } else {
            ExistingDayFields(uiState = uiState, viewModel = viewModel)
        }

        FooterSection(onConfirm = onConfirm)
    }
}

@Composable
private fun ProjectNameField(name: String) {
    OutlinedTextField(
        value = name,
        onValueChange = {},
        label = { Text(stringResource(R.string.project_name)) },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        enabled = false,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
        )
    )
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
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
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
        AddTimeRow(
            uiState.dailyWorkTime,
            R.string.daily_work_time,
            viewModel::currentDailyWorkTime,
            viewModel::setDailyWorkTime
        )
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
        AddTimeRow(
            uiState.workTimeToday,
            R.string.work_time_today,
            viewModel::currentWorkTimeToday,
            viewModel::setWorkTimeToday
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        AddTimeRow(uiState.lunchStart, R.string.lunch_start, viewModel::currentLunchStart, viewModel::setLunchStart)
        AddTimeRow(uiState.lunchEnd, R.string.lunch_end, viewModel::currentLunchEnd, viewModel::setLunchEnd)
        AddTimeRow(uiState.breakStart, R.string.break_start, viewModel::currentBreakStart, viewModel::setBreakStart)
        AddTimeRow(uiState.breakEnd, R.string.break_end, viewModel::currentBreakEnd, viewModel::setBreakEnd)

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        AddTimeRow(
            textFieldValue = uiState.dailyWorkTime,
            stringId = R.string.daily_work_time,
            currentTime = viewModel::currentDailyWorkTime,
            onConfirmation = viewModel::setDailyWorkTime
        )
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
private fun FooterSection(onConfirm: () -> Unit) {
    Button(
        onClick = onConfirm,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Text(stringResource(R.string.confirm), style = MaterialTheme.typography.titleLarge)
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
    val openTimePickerDialog = remember { mutableStateOf(false) }

    if (openTimePickerDialog.value) {
        TimePickerDialog(
            onDismissRequest = { openTimePickerDialog.value = false },
            onConfirmation = { time ->
                onConfirmation(time)
                openTimePickerDialog.value = false
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
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
            )
        )

        IconButton(onClick = currentTime) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = { openTimePickerDialog.value = true }) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

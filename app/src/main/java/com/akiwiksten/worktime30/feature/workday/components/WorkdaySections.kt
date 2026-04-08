package com.akiwiksten.worktime30.feature.workday.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.feature.workday.WorkdayUiState
import com.akiwiksten.worktime30.feature.workday.WorkdayViewModel

@Composable
fun ProjectNameField(name: String) {
    OutlinedTextField(
        value = name,
        onValueChange = {},
        label = { Text(text = stringResource(id = R.string.project_name)) },
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
fun HeaderSection(date: String, onClearDay: () -> Unit) {
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
            Button(
                onClick = onClearDay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(text = stringResource(id = R.string.clear_day))
            }
        }
    }
}

@Composable
fun NewDayFields(uiState: WorkdayUiState, viewModel: WorkdayViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
        AddTimeRow(
            textFieldValue = uiState.startTime,
            stringId = R.string.start_time,
            currentTime = viewModel::currentStartTime,
            onConfirmation = viewModel::setStartTime
        )
        AddTimeRow(
            textFieldValue = uiState.dailyWorkTime,
            stringId = R.string.daily_work_time,
            currentTime = viewModel::currentDailyWorkTime,
            onConfirmation = viewModel::setDailyWorkTime
        )
        AddTimeRow(
            textFieldValue = uiState.lunchTime,
            stringId = R.string.lunch_time,
            currentTime = viewModel::currentLunchTime,
            onConfirmation = viewModel::setLunchTime
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            Box(modifier = Modifier.weight(weight = 1f)) {
                AddCustomTimeRow(
                    customTime = uiState.balanceTotal,
                    customTimeFunction = viewModel::setBalanceTotal,
                    stringId = R.string.balance_total
                )
            }
            Box(modifier = Modifier.weight(weight = 1f)) {
                AddCustomTimeRow(
                    customTime = uiState.workTimeTotal,
                    customTimeFunction = viewModel::setWorkTimeTotal,
                    stringId = R.string.work_time_total
                )
            }
        }
        Text(
            text = stringResource(id = R.string.new_day),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun ExistingDayFields(uiState: WorkdayUiState, viewModel: WorkdayViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        MainWorkTimeFields(uiState = uiState, viewModel = viewModel)

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        LunchAndBreakFields(uiState = uiState, viewModel = viewModel)

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        DailySummaryFields(uiState = uiState, viewModel = viewModel)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        BalanceSummaryFields(uiState = uiState, viewModel = viewModel)
    }
}

@Composable
private fun MainWorkTimeFields(uiState: WorkdayUiState, viewModel: WorkdayViewModel) {
    AddTimeRow(
        textFieldValue = uiState.startTime,
        stringId = R.string.start_time,
        currentTime = viewModel::currentStartTime,
        onConfirmation = viewModel::setStartTime
    )
    AddTimeRow(
        textFieldValue = uiState.endTime,
        stringId = R.string.end_time,
        currentTime = viewModel::currentEndTime,
        onConfirmation = viewModel::setEndTime
    )
    AddTimeRow(
        textFieldValue = uiState.workTimeToday,
        stringId = R.string.work_time_today,
        currentTime = viewModel::currentWorkTimeToday,
        onConfirmation = viewModel::setWorkTimeToday
    )
}

@Composable
private fun LunchAndBreakFields(uiState: WorkdayUiState, viewModel: WorkdayViewModel) {
    AddTimeRow(
        textFieldValue = uiState.lunchStart,
        stringId = R.string.lunch_start,
        currentTime = viewModel::currentLunchStart,
        onConfirmation = viewModel::setLunchStart
    )
    AddTimeRow(
        textFieldValue = uiState.lunchEnd,
        stringId = R.string.lunch_end,
        currentTime = viewModel::currentLunchEnd,
        onConfirmation = viewModel::setLunchEnd
    )
    AddTimeRow(
        textFieldValue = uiState.breakStart,
        stringId = R.string.break_start,
        currentTime = viewModel::currentBreakStart,
        onConfirmation = viewModel::setBreakStart
    )
    AddTimeRow(
        textFieldValue = uiState.breakEnd,
        stringId = R.string.break_end,
        currentTime = viewModel::currentBreakEnd,
        onConfirmation = viewModel::setBreakEnd
    )
}

@Composable
private fun DailySummaryFields(uiState: WorkdayUiState, viewModel: WorkdayViewModel) {
    AddTimeRow(
        textFieldValue = uiState.dailyWorkTime,
        stringId = R.string.daily_work_time,
        currentTime = viewModel::currentDailyWorkTime,
        onConfirmation = viewModel::setDailyWorkTime
    )
    AddTimeRow(
        textFieldValue = uiState.lunchTime,
        stringId = R.string.lunch_time,
        currentTime = viewModel::currentLunchTime,
        onConfirmation = viewModel::setLunchTime
    )
}

@Composable
private fun BalanceSummaryFields(uiState: WorkdayUiState, viewModel: WorkdayViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            Box(modifier = Modifier.weight(weight = 1f)) {
                AddCustomTimeRow(
                    customTime = uiState.balanceToday,
                    customTimeFunction = viewModel::setBalanceToday,
                    stringId = R.string.balance_today
                )
            }
            Box(modifier = Modifier.weight(weight = 1f)) {
                AddCustomTimeRow(
                    customTime = uiState.balanceTotal,
                    customTimeFunction = viewModel::setBalanceTotal,
                    stringId = R.string.balance_total
                )
            }
        }
        AddCustomTimeRow(
            customTime = uiState.workTimeTotal,
            customTimeFunction = viewModel::setWorkTimeTotal,
            stringId = R.string.work_time_total
        )
    }
}

@Composable
fun FooterSection(onConfirm: () -> Unit) {
    Button(
        onClick = onConfirm,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Text(text = stringResource(id = R.string.confirm), style = MaterialTheme.typography.titleLarge)
    }
}

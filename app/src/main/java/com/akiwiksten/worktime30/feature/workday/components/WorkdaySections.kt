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
fun NewDayFields(uiState: WorkdayUiState.Success, actions: WorkdayFieldActions) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
        AddTimeRow(
            textFieldValue = uiState.startTime,
            stringId = R.string.start_time,
            currentTime = actions.onCurrentStartTime,
            onConfirmation = actions.onSetStartTime
        )
        AddTimeRow(
            textFieldValue = uiState.dailyWorkTime,
            stringId = R.string.daily_work_time,
            currentTime = actions.onCurrentDailyWorkTime,
            onConfirmation = actions.onSetDailyWorkTime
        )
        AddTimeRow(
            textFieldValue = uiState.lunchTime,
            stringId = R.string.lunch_time,
            currentTime = actions.onCurrentLunchTime,
            onConfirmation = actions.onSetLunchTime
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            Box(modifier = Modifier.weight(weight = 1f)) {
                AddCustomTimeRow(
                    customTime = uiState.balanceTotal,
                    customTimeFunction = actions.onSetBalanceTotal,
                    stringId = R.string.balance_total
                )
            }
            Box(modifier = Modifier.weight(weight = 1f)) {
                AddCustomTimeRow(
                    customTime = uiState.workTimeTotal,
                    customTimeFunction = actions.onSetWorkTimeTotal,
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
fun ExistingDayFields(uiState: WorkdayUiState.Success, actions: WorkdayFieldActions) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
        MainWorkTimeFields(uiState = uiState, actions = actions)

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        LunchAndBreakFields(uiState = uiState, actions = actions)

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        DailySummaryFields(uiState = uiState, actions = actions)

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        BalanceSummaryFields(uiState = uiState, actions = actions)
    }
}

@Composable
private fun MainWorkTimeFields(uiState: WorkdayUiState.Success, actions: WorkdayFieldActions) {
    AddTimeRow(
        textFieldValue = uiState.startTime,
        stringId = R.string.start_time,
        currentTime = actions.onCurrentStartTime,
        onConfirmation = actions.onSetStartTime
    )
    AddTimeRow(
        textFieldValue = uiState.endTime,
        stringId = R.string.end_time,
        currentTime = actions.onCurrentEndTime,
        onConfirmation = actions.onSetEndTime
    )
    AddTimeRow(
        textFieldValue = uiState.projectTime,
        stringId = R.string.project_time,
        currentTime = actions.onCurrentProjectTime,
        onConfirmation = actions.onSetProjectTime
    )
}

@Composable
private fun LunchAndBreakFields(uiState: WorkdayUiState.Success, actions: WorkdayFieldActions) {
    AddTimeRow(
        textFieldValue = uiState.lunchStart,
        stringId = R.string.lunch_start,
        currentTime = actions.onCurrentLunchStart,
        onConfirmation = actions.onSetLunchStart
    )
    AddTimeRow(
        textFieldValue = uiState.lunchEnd,
        stringId = R.string.lunch_end,
        currentTime = actions.onCurrentLunchEnd,
        onConfirmation = actions.onSetLunchEnd
    )
    AddTimeRow(
        textFieldValue = uiState.breakStart,
        stringId = R.string.break_start,
        currentTime = actions.onCurrentBreakStart,
        onConfirmation = actions.onSetBreakStart
    )
    AddTimeRow(
        textFieldValue = uiState.breakEnd,
        stringId = R.string.break_end,
        currentTime = actions.onCurrentBreakEnd,
        onConfirmation = actions.onSetBreakEnd
    )
}

@Composable
private fun DailySummaryFields(uiState: WorkdayUiState.Success, actions: WorkdayFieldActions) {
    AddTimeRow(
        textFieldValue = uiState.dailyWorkTime,
        stringId = R.string.daily_work_time,
        currentTime = actions.onCurrentDailyWorkTime,
        onConfirmation = actions.onSetDailyWorkTime
    )
    AddTimeRow(
        textFieldValue = uiState.lunchTime,
        stringId = R.string.lunch_time,
        currentTime = actions.onCurrentLunchTime,
        onConfirmation = actions.onSetLunchTime
    )
}

@Composable
private fun BalanceSummaryFields(uiState: WorkdayUiState.Success, actions: WorkdayFieldActions) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            Box(modifier = Modifier.weight(weight = 1f)) {
                AddCustomTimeRow(
                    customTime = uiState.balanceToday,
                    customTimeFunction = actions.onSetBalanceToday,
                    stringId = R.string.balance_today
                )
            }
            Box(modifier = Modifier.weight(weight = 1f)) {
                AddCustomTimeRow(
                    customTime = uiState.balanceTotal,
                    customTimeFunction = actions.onSetBalanceTotal,
                    stringId = R.string.balance_total
                )
            }
        }
        AddCustomTimeRow(
            customTime = uiState.workTimeTotal,
            customTimeFunction = actions.onSetWorkTimeTotal,
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

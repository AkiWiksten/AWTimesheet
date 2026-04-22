package com.akiwiksten.worktime30.feature.projects.daily.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.TimePickerDialog
import com.akiwiksten.worktime30.feature.projects.daily.ProjectsHeaderActions
import com.akiwiksten.worktime30.feature.projects.daily.WorkStatsEditorState

@Composable
internal fun ProjectsHeader(
    date: String,
    workTime: String,
    balanceToday: String,
    workStatsEditorState: WorkStatsEditorState,
    headerActions: ProjectsHeaderActions
) {
    val openDailyWorkTimePicker = remember { mutableStateOf(value = false) }

    if (openDailyWorkTimePicker.value) {
        TimePickerDialog(
            onDismissRequest = { openDailyWorkTimePicker.value = false },
            onConfirmation = { time ->
                headerActions.onDailyWorkTimeChange(time)
                openDailyWorkTimePicker.value = false
            },
            time = workStatsEditorState.dailyWorkTime,
            titleId = R.string.daily_work_time
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        ProjectsHeaderTitleCard(date = date)
        ProjectsHeaderStatsCard(
            workTime = workTime,
            balanceToday = balanceToday,
            workStatsEditorState = workStatsEditorState,
            onDailyWorkTimePickerClick = { openDailyWorkTimePicker.value = true },
            onBalanceTotalChange = headerActions.onBalanceTotalChange,
            onSaveWorkStats = headerActions.onSaveWorkStats
        )
    }
}

@Composable
private fun ProjectsHeaderTitleCard(date: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            Header(title = stringResource(id = R.string.projects_customers))
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ProjectsHeaderStatsCard(
    workTime: String,
    balanceToday: String,
    workStatsEditorState: WorkStatsEditorState,
    onDailyWorkTimePickerClick: () -> Unit,
    onBalanceTotalChange: (String) -> Unit,
    onSaveWorkStats: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            Text(
                text = "${stringResource(id = R.string.work_time_today)}: $workTime",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "${stringResource(id = R.string.flex_time_today)}: $balanceToday",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DailyWorkTimePickerRow(
                dailyWorkTime = workStatsEditorState.dailyWorkTime,
                isError = workStatsEditorState.isDailyWorkTimeError,
                onPickerClick = onDailyWorkTimePickerClick
            )
            OutlinedTextField(
                value = workStatsEditorState.balanceTotal,
                onValueChange = onBalanceTotalChange,
                label = { Text(text = stringResource(id = R.string.flex_time_total)) },
                singleLine = true,
                isError = workStatsEditorState.isBalanceTotalError,
                modifier = Modifier.fillMaxWidth()
            )
            SaveWorkStatsButton(
                isEnabled = !workStatsEditorState.isDailyWorkTimeError &&
                    !workStatsEditorState.isBalanceTotalError &&
                    workStatsEditorState.hasUnsavedChanges,
                onClick = onSaveWorkStats,
                modifier = Modifier.align(alignment = Alignment.End)
            )
        }
    }
}

@Composable
private fun DailyWorkTimePickerRow(
    dailyWorkTime: String,
    isError: Boolean,
    onPickerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        OutlinedTextField(
            value = dailyWorkTime,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = stringResource(id = R.string.daily_work_time)) },
            singleLine = true,
            isError = isError,
            modifier = Modifier.weight(weight = 1f)
        )
        IconButton(onClick = onPickerClick) {
            Icon(imageVector = Icons.Default.AccessTime, contentDescription = null)
        }
    }
}

@Composable
private fun SaveWorkStatsButton(
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        modifier = modifier,
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Icon(imageVector = Icons.Default.Save, contentDescription = null)
        Spacer(modifier = Modifier.width(width = 8.dp))
        Text(text = stringResource(id = R.string.save))
    }
}


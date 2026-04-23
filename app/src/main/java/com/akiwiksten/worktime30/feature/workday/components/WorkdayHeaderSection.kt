package com.akiwiksten.worktime30.feature.workday.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.akiwiksten.worktime30.core.ui.verticalScrollbar
import com.akiwiksten.worktime30.feature.workday.WorkdayHeaderActions
import com.akiwiksten.worktime30.feature.workday.WorkStatsEditorState

private const val FONT_SIZE_SCALE = 1.08f
private val STATS_FIELD_SPACING = 8.dp
private val STATS_CARD_CONTENT_PADDING = 10.dp
private val STATS_CARD_MAX_HEIGHT = 200.dp
private val SAVE_BUTTON_TOP_PADDING = 4.dp

@Composable
internal fun WorkdayHeader(
    date: String,
    workTime: String,
    flexTimeToday: String,
    workStatsEditorState: WorkStatsEditorState,
    headerActions: WorkdayHeaderActions
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
        verticalArrangement = Arrangement.spacedBy(space = 6.dp)
    ) {
        WorkdayHeaderTitleCard(date = date)
        WorkdayHeaderStatsCard(
            workTime = workTime,
            flexTimeToday = flexTimeToday,
            workStatsEditorState = workStatsEditorState,
            onDailyWorkTimePickerClick = { openDailyWorkTimePicker.value = true },
            onFlexTimeTotalChange = headerActions.onFlexTimeTotalChange,
            onSaveWorkStats = headerActions.onSaveWorkStats
        )
    }
}

@Composable
private fun WorkdayHeaderTitleCard(date: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(all = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 6.dp)
        ) {
            Header(title = stringResource(id = R.string.workday))
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = MaterialTheme.typography.headlineMedium.fontSize * FONT_SIZE_SCALE
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun WorkdayHeaderStatsCard(
    workTime: String,
    flexTimeToday: String,
    workStatsEditorState: WorkStatsEditorState,
    onDailyWorkTimePickerClick: () -> Unit,
    onFlexTimeTotalChange: (String) -> Unit,
    onSaveWorkStats: () -> Unit
) {
    val scrollState = rememberScrollState()

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = STATS_CARD_MAX_HEIGHT),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScrollbar(scrollState = scrollState)
                .verticalScroll(state = scrollState)
                .padding(all = STATS_CARD_CONTENT_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = STATS_FIELD_SPACING)
        ) {
            Text(
                text = "${stringResource(id = R.string.work_time_today)}: $workTime",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = MaterialTheme.typography.titleMedium.fontSize * FONT_SIZE_SCALE,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.secondary
            )
            Text(
                text = "${stringResource(id = R.string.flex_time_today)}: $flexTimeToday",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * FONT_SIZE_SCALE,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DailyWorkTimePickerRow(
                dailyWorkTime = workStatsEditorState.dailyWorkTime,
                isError = workStatsEditorState.isDailyWorkTimeError,
                onPickerClick = onDailyWorkTimePickerClick
            )
            FlexTimeTotalField(
                flexTimeTotal = workStatsEditorState.flexTimeTotal,
                isError = workStatsEditorState.isFlexTimeTotalError,
                onValueChange = onFlexTimeTotalChange
            )
            SaveWorkStatsButton(
                isEnabled = !workStatsEditorState.isDailyWorkTimeError &&
                    !workStatsEditorState.isFlexTimeTotalError &&
                    workStatsEditorState.hasUnsavedChanges,
                onClick = onSaveWorkStats,
                modifier = Modifier
                    .align(alignment = Alignment.End)
                    .padding(top = SAVE_BUTTON_TOP_PADDING)
            )
        }
    }
}

@Composable
private fun FlexTimeTotalField(
    flexTimeTotal: String,
    isError: Boolean,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = flexTimeTotal,
        onValueChange = onValueChange,
        label = {
            Text(
                text = stringResource(id = R.string.flex_time_total),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * FONT_SIZE_SCALE,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = MaterialTheme.typography.bodyLarge.fontSize * FONT_SIZE_SCALE
        ),
        singleLine = true,
        isError = isError,
        modifier = Modifier.fillMaxWidth()
    )
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
            label = {
                Text(
                    text = stringResource(id = R.string.daily_work_time),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * FONT_SIZE_SCALE,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize * FONT_SIZE_SCALE
            ),
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




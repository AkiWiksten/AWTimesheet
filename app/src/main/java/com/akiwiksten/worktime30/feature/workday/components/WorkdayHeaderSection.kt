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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.akiwiksten.worktime30.core.FIELD_CORNER_RADIUS
import com.akiwiksten.worktime30.core.FORM_INLINE_SPACING
import com.akiwiksten.worktime30.core.FORM_SECTION_SPACING
import com.akiwiksten.worktime30.core.HEADER_CONTENT_PADDING
import com.akiwiksten.worktime30.core.HEADER_CONTENT_SPACING
import com.akiwiksten.worktime30.core.LABEL_FONT_SIZE_SCALE
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.TimePickerDialog
import com.akiwiksten.worktime30.core.ui.verticalScrollbar
import com.akiwiksten.worktime30.feature.workday.WorkStatsEditorState
import com.akiwiksten.worktime30.feature.workday.WorkdayHeaderActions

private val STATS_CARD_MAX_HEIGHT = 200.dp
private val SAVE_BUTTON_TOP_PADDING = 4.dp

@Composable
internal fun WorkdayHeader(
    date: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = HEADER_CONTENT_SPACING)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(all = HEADER_CONTENT_PADDING),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = HEADER_CONTENT_SPACING)
            ) {
                Header(title = stringResource(id = R.string.workday))
                Text(
                    text = date,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize * LABEL_FONT_SIZE_SCALE
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
internal fun WorkdayStatsCard(
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

    WorkdayStatsCardContent(
        workTime = workTime,
        flexTimeToday = flexTimeToday,
        workStatsEditorState = workStatsEditorState,
        onDailyWorkTimePickerClick = { openDailyWorkTimePicker.value = true },
        onFlexTimeTotalChange = headerActions.onFlexTimeTotalChange,
        onSaveWorkStats = headerActions.onSaveWorkStats
    )
}

@Suppress("LongParameterList")
@Composable
private fun WorkdayStatsCardContent(
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
                .padding(all = FORM_SECTION_SPACING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)
        ) {
            Text(
                text = "${stringResource(id = R.string.work_time_today)}: $workTime",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${stringResource(id = R.string.flex_time_today)}: $flexTimeToday",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
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
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
            fontWeight = FontWeight.Bold
        ),
        singleLine = true,
        isError = isError,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS)
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
        horizontalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)
    ) {
        OutlinedTextField(
            value = dailyWorkTime,
            onValueChange = {},
            enabled = false,
            label = {
                Text(
                    text = stringResource(id = R.string.daily_work_time),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                fontWeight = FontWeight.Bold
            ),
            singleLine = true,
            isError = isError,
            modifier = Modifier.weight(weight = 1f),
            shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS)
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

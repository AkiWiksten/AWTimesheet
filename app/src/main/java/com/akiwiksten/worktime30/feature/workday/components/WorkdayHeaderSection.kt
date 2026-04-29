package com.akiwiksten.worktime30.feature.workday.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
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
import com.akiwiksten.worktime30.feature.workday.SettingsEditorState
import com.akiwiksten.worktime30.feature.workday.WorkdayHeaderActions

private val STATS_CARD_MAX_HEIGHT = 200.dp

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
    calculatedFlexTimeTotal: String,
    settingsEditorState: SettingsEditorState,
    headerActions: WorkdayHeaderActions
) {
    val openWorkTimeTodayEstimatePicker = remember { mutableStateOf(value = false) }

    if (openWorkTimeTodayEstimatePicker.value) {
        TimePickerDialog(
            onDismissRequest = { openWorkTimeTodayEstimatePicker.value = false },
            onConfirmation = { time ->
                headerActions.onWorkTimeTodayEstimateChange(time)
                openWorkTimeTodayEstimatePicker.value = false
            },
            time = settingsEditorState.workTimeTodayEstimate,
            titleId = R.string.work_time_today_estimate
        )
    }

    WorkdayStatsCardContent(
        params = WorkdayStatsCardContentParams(
            workTime = workTime,
            flexTimeToday = flexTimeToday,
            calculatedFlexTimeTotal = calculatedFlexTimeTotal,
            settingsEditorState = settingsEditorState,
            onWorkTimeTodayEstimatePickerClick = { openWorkTimeTodayEstimatePicker.value = true }
        )
    )
}

@Composable
private fun WorkdayStatsCardContent(
    params: WorkdayStatsCardContentParams
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = STATS_CARD_MAX_HEIGHT),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(all = FORM_SECTION_SPACING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)
        ) {
            WorkdayStatsSummaryTexts(
                workTime = params.workTime,
                flexTimeToday = params.flexTimeToday,
                calculatedFlexTimeTotal = params.calculatedFlexTimeTotal
            )
            WorkTimeTodayEstimatePickerRow(
                workTimeTodayEstimate = params.settingsEditorState.workTimeTodayEstimate,
                isError = params.settingsEditorState.isWorkTimeTodayEstimateError,
                onPickerClick = params.onWorkTimeTodayEstimatePickerClick
            )
        }
    }
}

@Composable
private fun WorkdayStatsSummaryTexts(
    workTime: String,
    flexTimeToday: String,
    calculatedFlexTimeTotal: String
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
    val calculatedFlexLabel = stringResource(id = R.string.flex_time_total_initial_calculated)
    Text(
        text = "$calculatedFlexLabel: $calculatedFlexTimeTotal",
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private data class WorkdayStatsCardContentParams(
    val workTime: String,
    val flexTimeToday: String,
    val calculatedFlexTimeTotal: String,
    val settingsEditorState: SettingsEditorState,
    val onWorkTimeTodayEstimatePickerClick: () -> Unit
)

@Composable
private fun WorkTimeTodayEstimatePickerRow(
    workTimeTodayEstimate: String,
    isError: Boolean,
    onPickerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)
    ) {
        OutlinedTextField(
            value = workTimeTodayEstimate,
            onValueChange = {},
            enabled = false,
            label = {
                Text(
                    text = stringResource(id = R.string.work_time_today_estimate),
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

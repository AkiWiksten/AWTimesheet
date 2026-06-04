package com.akiwiksten.awtimesheet.feature.workday.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.FORM_INLINE_SPACING
import com.akiwiksten.awtimesheet.core.FORM_SECTION_SPACING
import com.akiwiksten.awtimesheet.core.HEADER_CONTENT_PADDING
import com.akiwiksten.awtimesheet.core.HEADER_CONTENT_SPACING
import com.akiwiksten.awtimesheet.core.LABEL_FONT_SIZE_SCALE
import com.akiwiksten.awtimesheet.core.ui.Header
import com.akiwiksten.awtimesheet.core.ui.TimePickerDialog
import com.akiwiksten.awtimesheet.feature.workday.R
import com.akiwiksten.awtimesheet.feature.workday.WorkdayHeaderActions
import com.akiwiksten.awtimesheet.feature.workday.WorkdaySettingsEditorState

@Composable
internal fun WorkdayHeaderSection(
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
    state: WorkdayStatsCardState,
    headerActions: WorkdayHeaderActions
) {
    val openWorkTimeByDateEstimatePicker = remember { mutableStateOf(value = false) }

    if (openWorkTimeByDateEstimatePicker.value) {
        TimePickerDialog(
            onDismissRequest = { openWorkTimeByDateEstimatePicker.value = false },
            onConfirmation = { time ->
                headerActions.onWorkTimeByDateEstimateChange(time)
                openWorkTimeByDateEstimatePicker.value = false
            },
            time = state.editorState.workTimeByDateEstimate,
            titleId = R.string.work_time_by_date_estimate
        )
    }

    WorkdayStatsCardContent(
        params = WorkdayStatsCardContentParams(
            workTime = state.workTime,
            flexTimeByDate = state.flexTimeByDate,
            calculatedFlexTimeTotal = state.calculatedFlexTimeTotal,
            editorState = state.editorState,
            onWorkTimeByDateEstimatePickerClick = { openWorkTimeByDateEstimatePicker.value = true }
        )
    )
}

internal data class WorkdayStatsCardState(
    val workTime: String,
    val flexTimeByDate: String,
    val calculatedFlexTimeTotal: String,
    val editorState: WorkdaySettingsEditorState
)

@Composable
private fun WorkdayStatsCardContent(
    params: WorkdayStatsCardContentParams
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
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
                flexTimeByDate = params.flexTimeByDate,
                calculatedFlexTimeTotal = params.calculatedFlexTimeTotal
            )
            WorkTimeByDateEstimatePickerRow(
                workTimeByDateEstimate = params.editorState.workTimeByDateEstimate,
                isError = params.editorState.isWorkTimeByDateEstimateError,
                onPickerClick = params.onWorkTimeByDateEstimatePickerClick
            )
        }
    }
}

@Composable
private fun WorkdayStatsSummaryTexts(
    workTime: String,
    flexTimeByDate: String,
    calculatedFlexTimeTotal: String
) {
    Text(
        text = "${stringResource(id = R.string.work_time_by_date)}: $workTime",
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
            fontWeight = FontWeight.Bold
        ),
        color = MaterialTheme.colorScheme.onSurface
    )
    Text(
        text = "${stringResource(id = R.string.flex_time_by_date)}: $flexTimeByDate",
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
    val flexTimeByDate: String,
    val calculatedFlexTimeTotal: String,
    val editorState: WorkdaySettingsEditorState,
    val onWorkTimeByDateEstimatePickerClick: () -> Unit
)

@Composable
private fun WorkTimeByDateEstimatePickerRow(
    workTimeByDateEstimate: String,
    isError: Boolean,
    onPickerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)
    ) {
        OutlinedTextField(
            value = workTimeByDateEstimate,
            onValueChange = {},
            enabled = false,
            label = {
                Text(
                    text = stringResource(id = R.string.work_time_by_date_estimate),
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

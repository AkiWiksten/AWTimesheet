package com.akiwiksten.awtimesheet.feature.workday.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.LABEL_FONT_SIZE_SCALE
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.ui.Header
import com.akiwiksten.awtimesheet.core.ui.TimePickerDialog
import com.akiwiksten.awtimesheet.feature.workday.R
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayActionButtonsState
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayHeaderActions
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayListItemUiModel
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayStatsCardContentParams
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayStatsCardState

@Composable
internal fun WorkdayHeaderSection(
    date: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION)
        ) {
            Column(
                modifier = Modifier.padding(all = PADDING_SPACING_SMALL),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
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
internal fun WorkdayStatsSection(
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

    WorkdayStatsSectionContent(
        params = WorkdayStatsCardContentParams(
            workTime = state.workTime,
            flexTimeByDate = state.flexTimeByDate,
            calculatedFlexTimeTotal = state.calculatedFlexTimeTotal,
            editorState = state.editorState,
            isTimePickerEnabled = state.isTimePickerEnabled,
            onWorkTimeByDateEstimatePickerClick = { openWorkTimeByDateEstimatePicker.value = true }
        )
    )
}

@Composable
private fun WorkdayStatsSectionContent(
    params: WorkdayStatsCardContentParams
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION)
    ) {
        Column(
            modifier = Modifier
                .padding(all = PADDING_SPACING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING)
        ) {
            WorkdayStatsSummaryTexts(
                workTime = params.workTime,
                flexTimeByDate = params.flexTimeByDate,
                calculatedFlexTimeTotal = params.calculatedFlexTimeTotal
            )
            WorkTimeByDateEstimatePickerRow(
                workTimeByDateEstimate = params.editorState.workTimeByDateEstimate,
                isError = params.editorState.isWorkTimeByDateEstimateError,
                isEnabled = params.isTimePickerEnabled,
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

@Composable
private fun WorkTimeByDateEstimatePickerRow(
    workTimeByDateEstimate: String,
    isError: Boolean,
    isEnabled: Boolean,
    onPickerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
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
        IconButton(onClick = onPickerClick, enabled = isEnabled) {
            Icon(imageVector = Icons.Default.AccessTime, contentDescription = null)
        }
    }
}

@Composable
internal fun WorkdayActionButtonsSection(
    state: WorkdayActionButtonsState,
    onAddClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val selectedProject = state.items.getOrNull(index = state.selectedIndex)
    val deleteButtonText = if (selectedProject?.projectTime != ZERO_TIME) {
        stringResource(id = R.string.nullify)
    } else {
        stringResource(id = R.string.delete)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
    ) {
        Button(
            onClick = onAddClick,
            enabled = !state.isAddEditDisabled,
            modifier = Modifier.weight(weight = 1f),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = DEFAULT_ELEVATION)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(size = 18.dp))
            Spacer(modifier = Modifier.width(width = PADDING_SPACING_SMALL))
            Text(text = stringResource(id = R.string.add))
        }
        Button(
            onClick = onEditClick,
            enabled = state.selectedIndex != -1 && !state.isAddEditDisabled,
            modifier = Modifier.weight(weight = 1f),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = DEFAULT_ELEVATION),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(size = 18.dp))
            Spacer(modifier = Modifier.width(width = PADDING_SPACING_SMALL))
            Text(text = stringResource(id = R.string.edit))
        }
        Button(
            onClick = onDeleteClick,
            enabled = state.selectedIndex != -1,
            modifier = Modifier.weight(weight = 1f),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = DEFAULT_ELEVATION),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(size = 18.dp))
            Spacer(modifier = Modifier.width(width = PADDING_SPACING_SMALL))
            Text(text = deleteButtonText)
        }
    }
}

@Composable
internal fun WorkdayListSection(
    items: List<WorkdayListItemUiModel>,
    selectedItemKey: String?,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS)
                )
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(space = 2.dp)
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.no_projects_available),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(all = PADDING_SPACING)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondary)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(space = 2.dp)
                ) {
                    items.forEach { item ->
                        ProjectListItem(
                            item = item,
                            isSelected = selectedItemKey == item.stableKey,
                            onClick = { onItemSelected(item.index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectListItem(
    item: WorkdayListItemUiModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = isSelected, onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(all = PADDING_SPACING),
            verticalArrangement = Arrangement.spacedBy(space = 4.dp)
        ) {
            if (item.isProjectNameOnlyPlaceholder) {
                ProjectNameOnlyContent(projectName = item.projectName)
            } else {
                ProjectSummaryContent(
                    projectName = item.projectName,
                    projectTime = item.projectTime,
                    kilometresLabel = item.kilometresLabel,
                    allowance = item.allowance,
                    workType = item.workType
                )
            }
        }
    }
}

@Composable
private fun ProjectNameOnlyContent(projectName: String) {
    Text(
        text = projectName,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ProjectSummaryContent(
    projectName: String,
    projectTime: String,
    kilometresLabel: String,
    allowance: String,
    workType: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = projectName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = projectTime,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            if (workType.isNotEmpty()) {
                Text(
                    text = workType,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (allowance.isNotEmpty()) {
                Text(
                    text = allowance,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Text(
            text = kilometresLabel,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

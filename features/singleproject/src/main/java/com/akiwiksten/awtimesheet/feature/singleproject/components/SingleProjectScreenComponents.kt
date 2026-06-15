package com.akiwiksten.awtimesheet.feature.singleproject.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.text.isDigitsOnly
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.LABEL_FONT_SIZE_SCALE
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.ui.AwtButton
import com.akiwiksten.awtimesheet.core.ui.DropdownMenuBox
import com.akiwiksten.awtimesheet.core.ui.DropdownMenuField
import com.akiwiksten.awtimesheet.core.ui.Header
import com.akiwiksten.awtimesheet.core.ui.TimePickerDialog
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.singleproject.R
import com.akiwiksten.awtimesheet.core.R as CoreR

@Composable
fun SingleProjectHeaderSection(date: String, workTimeByDate: String) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = PADDING_SPACING_SMALL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "${stringResource(id = CoreR.string.work_time_by_date)}: $workTimeByDate",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleProjectTopBar(onNavigateBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = DEFAULT_ELEVATION,
        shadowElevation = DEFAULT_ELEVATION
    ) {
        CenterAlignedTopAppBar(
            title = {
                Header(
                    title = stringResource(id = R.string.project_customer),
                    modifier = Modifier.padding(top = 0.dp)
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            },
            actions = {
                Spacer(modifier = Modifier.width(width = 48.dp))
            }
        )
    }
}

@Composable
fun SingleProjectTimePickerDialogSection(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit,
    currentTime: String
) {
    if (showDialog) {
        TimePickerDialog(
            onDismissRequest = onDismissRequest,
            onConfirmation = onConfirmation,
            titleId = CoreR.string.project_time,
            time = currentTime
        )
    }
}

@Composable
private fun ProjectTimeSelectionRow(
    state: SingleProjectState,
    onOpenProjectDetails: () -> Unit,
    onOpenTimePicker: () -> Unit,
    onStateChange: (SingleProjectState) -> Unit,
    isTimePickerDisabled: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = CardDefaults.outlinedCardBorder(enabled = true)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = PADDING_SPACING_SMALL),
            horizontalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProjectTimeReadOnlyField(
                projectTime = state.projectTime,
                onStateChange = { projectTime -> onStateChange(state.copy(projectTime = projectTime)) }
            )

            ProjectTimeActionsColumn(
                onOpenProjectDetails = onOpenProjectDetails,
                onOpenTimePicker = onOpenTimePicker,
                isTimePickerDisabled = isTimePickerDisabled
            )
        }
    }
}

@Composable
private fun RowScope.ProjectTimeReadOnlyField(
    projectTime: String,
    onStateChange: (String) -> Unit
) {
    OutlinedTextField(
        value = projectTime,
        onValueChange = onStateChange,
        label = {
            Text(
                text = stringResource(id = CoreR.string.project_time),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        modifier = Modifier.weight(weight = 1f),
        enabled = false,
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        leadingIcon = { Icon(imageVector = Icons.Default.AccessTime, contentDescription = null) },
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}

@Composable
private fun ProjectTimeActionsColumn(
    onOpenProjectDetails: () -> Unit,
    onOpenTimePicker: () -> Unit,
    isTimePickerDisabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)) {
        AwtButton(
            onClick = onOpenProjectDetails,
        ) {
            Icon(imageVector = Icons.Default.History, contentDescription = null)
            Spacer(modifier = Modifier.width(width = 4.dp))
            Text(text = stringResource(id = R.string.details))
        }

        Text(
            text = stringResource(id = R.string.or_text),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
        )

        AwtButton(
            onClick = onOpenTimePicker,
            enabled = !isTimePickerDisabled,
            modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(width = 4.dp))
            Text(text = stringResource(id = R.string.pick))
        }
    }
}

@Composable
internal fun SingleProjectTimeSelectionSection(
    state: SingleProjectState,
    onOpenProjectDetails: () -> Unit,
    onStateChange: (SingleProjectState) -> Unit,
    isTimePickerDisabled: Boolean
) {
    val openTimePickerDialogState = remember { mutableStateOf(false) }

    SingleProjectTimePickerDialogSection(
        showDialog = openTimePickerDialogState.value,
        onDismissRequest = { openTimePickerDialogState.value = false },
        onConfirmation = { time ->
            onStateChange(state.copy(projectTime = time))
            openTimePickerDialogState.value = false
        },
        currentTime = state.projectTime
    )

    ProjectTimeSelectionRow(
        state = state,
        onOpenProjectDetails = onOpenProjectDetails,
        onOpenTimePicker = { openTimePickerDialogState.value = true },
        onStateChange = onStateChange,
        isTimePickerDisabled = isTimePickerDisabled
    )
}

@Composable
internal fun SingleProjectProjectNameField(
    projectName: String,
    onProjectNameChange: (String) -> Unit,
    isEditable: Boolean,
    isError: Boolean
) {
    OutlinedTextField(
        value = projectName,
        onValueChange = onProjectNameChange,
        label = {
            Text(
                text = stringResource(id = CoreR.string.project_name),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = isEditable,
        singleLine = true,
        isError = isError,
        supportingText = if (isError) {
            { Text(text = stringResource(id = R.string.project_name_duplicate_error)) }
        } else {
            null
        },
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS)
    )
}

@Composable
internal fun SingleProjectDownSection(
    state: SingleProjectState,
    workTypeDropDownList: List<String>,
    isAbsence: Boolean,
    onStateChange: (SingleProjectState) -> Unit
) {
    DropdownMenuBox(
        items = workTypeDropDownList,
        field = DropdownMenuField(
            labelId = CoreR.string.work_type,
            selectedText = state.workType
        ),
        onItemSelected = { onStateChange(state.copy(workType = it)) }
    )

    DropdownMenuBox(
        items = listOf(
            stringResource(id = CoreR.string.no_allowance),
            stringResource(id = CoreR.string.full_allowance),
            stringResource(id = CoreR.string.half_day_allowance)
        ),
        field = DropdownMenuField(
            labelId = CoreR.string.allowance,
            selectedText = state.allowance,
            enabled = !isAbsence
        ),
        onItemSelected = { onStateChange(state.copy(allowance = it)) }
    )

    OutlinedTextField(
        value = state.kilometres,
        onValueChange = { if (it.isDigitsOnly()) onStateChange(state.copy(kilometres = it)) },
        label = {
            Text(
                text = stringResource(id = CoreR.string.kilometres),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = !isAbsence,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS)
    )
}

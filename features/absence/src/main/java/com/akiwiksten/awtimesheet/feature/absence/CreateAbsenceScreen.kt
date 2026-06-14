package com.akiwiksten.awtimesheet.feature.absence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.ui.AwtButton
import com.akiwiksten.awtimesheet.core.ui.DropdownMenuBox
import com.akiwiksten.awtimesheet.core.ui.DropdownMenuField
import com.akiwiksten.awtimesheet.core.ui.LocalContentBottomPadding
import com.akiwiksten.awtimesheet.domain.model.AbsenceState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAbsenceScreen(
    onNavigateBack: () -> Unit,
    onAbsenceCreated: (AbsenceState) -> Unit,
) {
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var absenceType by rememberSaveable { mutableStateOf("") }
    var includeWeekends by rememberSaveable { mutableStateOf(false) }
    var showStartDatePicker by rememberSaveable { mutableStateOf(false) }
    var showEndDatePicker by rememberSaveable { mutableStateOf(false) }

    AbsenceDatePickerDialogs(
        showStartPicker = showStartDatePicker,
        startDate = startDate,
        onStartDismiss = { showStartDatePicker = false },
        onStartDateSelected = {
            startDate = it
            if (endDate.isBefore(it)) {
                endDate = it
            }
            showStartDatePicker = false
        },
        showEndPicker = showEndDatePicker,
        endDate = endDate,
        onEndDismiss = { showEndDatePicker = false },
        onEndDateSelected = {
            endDate = it
            showEndDatePicker = false
        },
    )
    Scaffold(
        topBar = { CreateAbsenceTopBar(onNavigateBack = onNavigateBack) }
    ) { innerPadding ->
        CreateAbsenceContent(
            startDate = startDate,
            endDate = endDate,
            absenceType = absenceType,
            includeWeekends = includeWeekends,
            onAbsenceTypeChange = { absenceType = it },
            onIncludeWeekendsChange = { includeWeekends = it },
            onShowStartDatePicker = { showStartDatePicker = true },
            onShowEndDatePicker = { showEndDatePicker = true },
            onSave = {
                onAbsenceCreated(
                    AbsenceState(
                        absenceType = absenceType,
                        startDate = startDate.toString(),
                        endDate = endDate.toString(),
                        includeWeekends = includeWeekends
                    )
                )
                onNavigateBack()
            },
            modifier = Modifier.fillMaxWidth()
                .padding(innerPadding)
                .padding(all = PADDING_SPACING)
                .padding(bottom = LocalContentBottomPadding.current),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAbsenceTopBar(onNavigateBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Text(text = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.new_absence_title))
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.dismiss)
                )
            }
        }
    )
}

@Suppress("LongParameterList")
@Composable
private fun AbsenceDatePickerDialogs(
    showStartPicker: Boolean,
    startDate: LocalDate,
    onStartDismiss: () -> Unit,
    onStartDateSelected: (LocalDate) -> Unit,
    showEndPicker: Boolean,
    endDate: LocalDate,
    onEndDismiss: () -> Unit,
    onEndDateSelected: (LocalDate) -> Unit,
) {
    if (showStartPicker) {
        AbsenceDatePickerDialog(
            initialDate = startDate,
            onDismiss = onStartDismiss,
            onDateSelected = onStartDateSelected
        )
    }
    if (showEndPicker) {
        AbsenceDatePickerDialog(
            initialDate = endDate,
            onDismiss = onEndDismiss,
            onDateSelected = onEndDateSelected
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun CreateAbsenceContent(
    startDate: LocalDate,
    endDate: LocalDate,
    absenceType: String,
    includeWeekends: Boolean,
    onAbsenceTypeChange: (String) -> Unit,
    onIncludeWeekendsChange: (Boolean) -> Unit,
    onShowStartDatePicker: () -> Unit,
    onShowEndDatePicker: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING),
        horizontalAlignment = Alignment.Start
    ) {
        AbsenceFormCard(
            startDate = startDate,
            endDate = endDate,
            absenceType = absenceType,
            includeWeekends = includeWeekends,
            onAbsenceTypeChange = onAbsenceTypeChange,
            onIncludeWeekendsChange = onIncludeWeekendsChange,
            onShowStartDatePicker = onShowStartDatePicker,
            onShowEndDatePicker = onShowEndDatePicker,
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            AwtButton(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = absenceType.isNotBlank()
            ) {
                Text(text = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.save))
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun AbsenceFormCard(
    startDate: LocalDate,
    endDate: LocalDate,
    absenceType: String,
    includeWeekends: Boolean,
    onAbsenceTypeChange: (String) -> Unit,
    onIncludeWeekendsChange: (Boolean) -> Unit,
    onShowStartDatePicker: () -> Unit,
    onShowEndDatePicker: () -> Unit,
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = PADDING_SPACING),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING),
            horizontalAlignment = Alignment.Start
        ) {
            DatePickerRow(
                labelId = R.string.start_date,
                dateText = startDate.toString(),
                onPickDate = onShowStartDatePicker
            )
            DatePickerRow(
                labelId = R.string.end_date,
                dateText = endDate.toString(),
                onPickDate = onShowEndDatePicker
            )
            DropdownMenuBox(
                items = listOf(
                    stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_paid_vacation),
                    stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_unpaid_vacation),
                    stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_sick_leave),
                    stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_parental_leave),
                    stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_other_leave),
                    stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_flex_day)
                ),
                onItemSelected = onAbsenceTypeChange,
                field = DropdownMenuField(
                    labelId = R.string.absence_work_type,
                    selectedText = absenceType,
                    enabled = true
                )
            )
            WeekendsSection(
                includeWeekends = includeWeekends,
                onIncludeWeekendsChange = onIncludeWeekendsChange,
            )
        }
    }
}

@Composable
private fun WeekendsSection(
    includeWeekends: Boolean,
    onIncludeWeekendsChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
    ) {
        Text(text = stringResource(id = R.string.absence_weekends_title), fontWeight = FontWeight.Bold)
        WeekendsOptionRow(
            text = stringResource(id = R.string.no),
            selected = !includeWeekends,
            onClick = { onIncludeWeekendsChange(false) }
        )
        WeekendsOptionRow(
            text = stringResource(id = R.string.yes),
            selected = includeWeekends,
            onClick = { onIncludeWeekendsChange(true) }
        )
    }
}

@Composable
private fun WeekendsOptionRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbsenceDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis
                        ?.toLocalDateUtc()
                        ?.let(onDateSelected)
                        ?: onDismiss()
                }
            ) {
                Text(text = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.dismiss))
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun DatePickerRow(
    labelId: Int,
    dateText: String,
    onPickDate: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
    ) {
        OutlinedTextField(
            value = dateText,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = stringResource(id = labelId), fontWeight = FontWeight.Bold) },
            modifier = Modifier.weight(weight = 1f),
            textStyle = TextStyle(
                fontWeight = FontWeight.Bold
            )

        )
        IconButton(onClick = onPickDate) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.select_date)
            )
        }
    }
}

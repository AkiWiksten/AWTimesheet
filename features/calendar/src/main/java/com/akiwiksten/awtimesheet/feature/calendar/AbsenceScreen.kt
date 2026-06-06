package com.akiwiksten.awtimesheet.feature.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.ui.DropdownMenuBox
import com.akiwiksten.awtimesheet.core.ui.DropdownMenuField
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.material3.rememberDatePickerState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbsenceScreen(
    onNavigateBack: () -> Unit,
) {
    var showNewDialog by rememberSaveable { mutableStateOf(value = false) }
    val savedAbsences = remember { mutableStateListOf<SavedAbsence>() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = stringResource(id = R.string.absence)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.absence)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(all = PADDING_SPACING),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING),
            horizontalAlignment = Alignment.Start
        ) {
            ElevatedCard(
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = { showNewDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(all = PADDING_SPACING)
                ) {
                    Text(text = stringResource(id = R.string.new_absence))
                }
            }

            if (savedAbsences.isNotEmpty()) {
                Text(text = stringResource(id = R.string.saved_absences_title))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f),
                    verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
                ) {
                    items(items = savedAbsences) { savedAbsence ->
                        SavedAbsenceListItem(savedAbsence = savedAbsence)
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        AbsenceNewDialog(
            onDismiss = { showNewDialog = false },
            onSave = { workType, startDate, endDate ->
                savedAbsences.add(
                    element = SavedAbsence(
                        workType = workType,
                        startDate = startDate,
                        endDate = endDate
                    )
                )
                showNewDialog = false
            }
        )
    }
}

private data class SavedAbsence(
    val workType: String,
    val startDate: String,
    val endDate: String,
)

@Composable
private fun SavedAbsenceListItem(savedAbsence: SavedAbsence) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = PADDING_SPACING),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
        ) {
            Text(text = savedAbsence.workType)
            Text(text = "${stringResource(id = R.string.start_date)}: ${savedAbsence.startDate}")
            Text(text = "${stringResource(id = R.string.end_date)}: ${savedAbsence.endDate}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbsenceNewDialog(
    onDismiss: () -> Unit,
    onSave: (workType: String, startDate: String, endDate: String) -> Unit,
) {
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var workType by rememberSaveable { mutableStateOf("") }
    var includeWeekends by rememberSaveable { mutableStateOf(false) }
    var showStartDatePicker by rememberSaveable { mutableStateOf(false) }
    var showEndDatePicker by rememberSaveable { mutableStateOf(false) }

    if (showStartDatePicker) {
        AbsenceDatePickerDialog(
            initialDate = startDate,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = {
                startDate = it
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        AbsenceDatePickerDialog(
            initialDate = endDate,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = {
                endDate = it
                showEndDatePicker = false
            }
        )
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        ElevatedCard(
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = PADDING_SPACING),
                verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = stringResource(id = R.string.new_absence_title))

                DatePickerRow(
                    labelId = R.string.start_date,
                    dateText = startDate.toString(),
                    onPickDate = { showStartDatePicker = true }
                )
                DatePickerRow(
                    labelId = R.string.end_date,
                    dateText = endDate.toString(),
                    onPickDate = { showEndDatePicker = true }
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
                    onItemSelected = { workType = it },
                    field = DropdownMenuField(
                        labelId = R.string.absence_work_type,
                        selectedText = workType,
                        enabled = true
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
                ) {
                    Text(text = stringResource(id = R.string.absence_weekends_title))

                    WeekendsOptionRow(
                        text = stringResource(id = R.string.no),
                        selected = !includeWeekends,
                        onClick = { includeWeekends = false }
                    )
                    WeekendsOptionRow(
                        text = stringResource(id = R.string.yes),
                        selected = includeWeekends,
                        onClick = { includeWeekends = true }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL, alignment = Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.dismiss))
                    }
                    Button(
                        onClick = {
                            onSave(workType, startDate.toString(), endDate.toString())
                        },
                        enabled = workType.isNotBlank()
                    ) {
                        Text(text = stringResource(id = R.string.save))
                    }
                }
            }
        }
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
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(text = text)
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
                Text(text = stringResource(id = R.string.confirm))
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
            label = {
                Text(text = stringResource(id = labelId))
            },
            modifier = Modifier.weight(weight = 1f)
        )
        IconButton(onClick = onPickDate) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = stringResource(id = R.string.select_date)
            )
        }
    }
}

private fun LocalDate.toUtcMillis(): Long {
    return atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
}

private fun Long.toLocalDateUtc(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}





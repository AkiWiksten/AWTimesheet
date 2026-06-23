package com.akiwiksten.awtimesheet.feature.absence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.ui.AwtButton
import com.akiwiksten.awtimesheet.core.ui.AwtCenterAlignedTopAppBar
import com.akiwiksten.awtimesheet.core.ui.DropdownMenuBox
import com.akiwiksten.awtimesheet.core.ui.DropdownMenuField
import com.akiwiksten.awtimesheet.core.ui.LocalContentBottomPadding
import com.akiwiksten.awtimesheet.domain.model.AbsenceState
import java.time.LocalDate

@Composable
fun CreateAbsenceScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateAbsenceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CreateAbsenceScreenStateful(
        existingAbsences = uiState.existingAbsences,
        onNavigateBack = onNavigateBack,
        onSaveAbsence = viewModel::addAbsence,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAbsenceScreenStateful(
    existingAbsences: List<SavedAbsence>,
    onNavigateBack: () -> Unit,
    onSaveAbsence: (AbsenceState, () -> Unit) -> Unit,
) {
    var startDate by remember { mutableStateOf(value = LocalDate.now()) }
    var endDate by remember { mutableStateOf(value = LocalDate.now()) }
    var absenceType by rememberSaveable { mutableStateOf(value = "") }
    var includeWeekends by rememberSaveable { mutableStateOf(value = false) }
    var showStartPicker by rememberSaveable { mutableStateOf(value = false) }
    var showEndPicker by rememberSaveable { mutableStateOf(value = false) }
    val isOverlap = remember(startDate, endDate, existingAbsences) {
        isDateOverlap(startDate, endDate, existingAbsences)
    }
    AbsenceDatePickerDialogs(
        showStartPicker = showStartPicker,
        startDate = startDate,
        onStartDismiss = { showStartPicker = false },
        onStartDateSelected = {
            startDate = it
            if (endDate.isBefore(it)) endDate = it
            showStartPicker = false
        },
        showEndPicker = showEndPicker,
        endDate = endDate,
        onEndDismiss = { showEndPicker = false },
    ) {
        endDate = it
        showEndPicker = false
    }
    val flexDayType = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_flex_day)
    CreateAbsenceContent(
        onNavigateBack = onNavigateBack,
        state = CreateAbsenceFormState(
            startDate = startDate,
            endDate = endDate,
            absenceType = absenceType,
            includeWeekends = includeWeekends,
            isOverlap = isOverlap,
        ),
        onAbsenceTypeChange = { absenceType = it },
        onIncludeWeekendsChange = { includeWeekends = it },
        onShowStartDatePicker = { showStartPicker = true },
        onShowEndDatePicker = { showEndPicker = true },
        onSave = {
            onSaveAbsence(
                AbsenceState(
                    id = 0,
                    absenceType = absenceType,
                    startDate = startDate.toString(),
                    endDate = endDate.toString(),
                    includeWeekends = includeWeekends,
                    isFlexDay = absenceType == flexDayType
                ),
                onNavigateBack
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = PADDING_SPACING),
    )
}

private fun isDateOverlap(startDate: LocalDate, endDate: LocalDate, existingAbsences: List<SavedAbsence>): Boolean {
    return existingAbsences.any { existing ->
        val exStart = LocalDate.parse(existing.startDate)
        val exEnd = LocalDate.parse(existing.endDate)
        !startDate.isAfter(exEnd) && !exStart.isAfter(endDate)
    }
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
            titleResId = R.string.select_date_start,
            onDismiss = onStartDismiss,
            onDateSelected = onStartDateSelected
        )
    }
    if (showEndPicker) {
        AbsenceDatePickerDialog(
            initialDate = endDate,
            titleResId = R.string.select_date_end,
            onDismiss = onEndDismiss,
            onDateSelected = onEndDateSelected
        )
    }
}

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAbsenceContent(
    onNavigateBack: () -> Unit,
    state: CreateAbsenceFormState,
    onAbsenceTypeChange: (String) -> Unit,
    onIncludeWeekendsChange: (Boolean) -> Unit,
    onShowStartDatePicker: () -> Unit,
    onShowEndDatePicker: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            AwtCenterAlignedTopAppBar(
                title = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.new_absence_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.dismiss),
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier.padding(paddingValues = innerPadding),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING),
            horizontalAlignment = Alignment.Start,
        ) {
            AbsenceFormCard(
                state = state,
                onAbsenceTypeChange = onAbsenceTypeChange,
                onIncludeWeekendsChange = onIncludeWeekendsChange,
                onShowStartDatePicker = onShowStartDatePicker,
                onShowEndDatePicker = onShowEndDatePicker,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                AwtButton(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.absenceType.isNotBlank() && !state.isOverlap,
                ) {
                    Text(text = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.save))
                }
            }
            Spacer(modifier = Modifier.padding(bottom = LocalContentBottomPadding.current))
        }
    }
}

@Composable
private fun AbsenceFormCard(
    state: CreateAbsenceFormState,
    onAbsenceTypeChange: (String) -> Unit,
    onIncludeWeekendsChange: (Boolean) -> Unit,
    onShowStartDatePicker: () -> Unit,
    onShowEndDatePicker: () -> Unit,
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = PADDING_SPACING),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING),
            horizontalAlignment = Alignment.Start,
        ) {
            DatePickerRow(
                labelId = R.string.start_date,
                dateText = state.startDate.toString(),
                onPickDate = onShowStartDatePicker,
            )
            DatePickerRow(
                labelId = R.string.end_date,
                dateText = state.endDate.toString(),
                onPickDate = onShowEndDatePicker,
            )

            if (state.isOverlap) {
                Text(
                    text = stringResource(id = R.string.error_overlap),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                )
            }

            DropdownMenuBox(
                items = listOf(
                    stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_paid_vacation),
                    stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_unpaid_vacation),
                    stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_sick_leave),
                    stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_parental_leave),
                    stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_other_leave),
                    stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_flex_day),
                ),
                onItemSelected = onAbsenceTypeChange,
                field = DropdownMenuField(
                    labelId = R.string.absence_work_type,
                    selectedText = state.absenceType,
                    enabled = true,
                ),
            )
            WeekendsSection(
                includeWeekends = state.includeWeekends,
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
        listOf(
            stringResource(id = R.string.no) to false,
            stringResource(id = R.string.yes) to true
        ).forEach { (text, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = includeWeekends == value,
                        onClick = { onIncludeWeekendsChange(value) },
                        role = Role.RadioButton
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
            ) {
                RadioButton(selected = includeWeekends == value, onClick = { onIncludeWeekendsChange(value) })
                Text(text = text, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbsenceDatePickerDialog(
    initialDate: LocalDate,
    titleResId: Int,
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
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = stringResource(id = titleResId),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            }
        )
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

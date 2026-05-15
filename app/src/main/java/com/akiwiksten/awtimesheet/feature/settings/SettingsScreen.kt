@file:Suppress("TooManyFunctions")

package com.akiwiksten.awtimesheet.feature.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.core.ACTION_BUTTON_FONT_SIZE
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.FORM_GROUP_SPACING
import com.akiwiksten.awtimesheet.core.FORM_INLINE_SPACING
import com.akiwiksten.awtimesheet.core.FORM_MAX_WIDTH
import com.akiwiksten.awtimesheet.core.FORM_SECTION_SPACING
import com.akiwiksten.awtimesheet.core.HEADER_CONTENT_PADDING
import com.akiwiksten.awtimesheet.core.HEADER_CONTENT_SPACING
import com.akiwiksten.awtimesheet.core.LABEL_FONT_SIZE_SCALE
import com.akiwiksten.awtimesheet.core.SCREEN_CONTENT_SPACING
import com.akiwiksten.awtimesheet.core.ui.AddTextFieldDialog
import com.akiwiksten.awtimesheet.core.ui.DropdownMenuBox
import com.akiwiksten.awtimesheet.core.ui.Header
import com.akiwiksten.awtimesheet.core.ui.TimePickerDialog
import com.akiwiksten.awtimesheet.core.ui.hasChanges
import com.akiwiksten.awtimesheet.core.ui.isActionEnabled
import com.akiwiksten.awtimesheet.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.awtimesheet.core.ui.verticalScrollbar
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import kotlinx.coroutines.flow.collectLatest

private val INITIAL_FLEX_TIME_TOTAL_INPUT_REGEX = Regex(pattern = "[+-]?(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val ctx = LocalContext.current
    val defaultWorkType = stringResource(id = R.string.other)
    val noProjectsMessage = stringResource(id = R.string.no_projects_available)

    LaunchedEffect(Unit) {
        settingsViewModel.loadSettings()
    }

    LaunchedEffect(uiState, defaultWorkType) {
        if (uiState is SettingsUiState.Success) {
            settingsViewModel.ensureDefaultWorkType(defaultWorkType)
        }
    }

    LaunchedEffect(settingsViewModel, ctx, noProjectsMessage) {
        settingsViewModel.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.MonthlyReportReady -> {
                    generateReport(
                        ctx = ctx,
                        projectsByMonth = event.projectsByMonth,
                        endOfMonthDate = event.endOfMonthDate,
                        name = event.name,
                        employer = event.employer
                    )
                }
                is SettingsEvent.MonthlyReportError -> {
                    Toast.makeText(ctx, event.message, Toast.LENGTH_SHORT).show()
                }
                is SettingsEvent.NoProjectsForMonth -> {
                    Toast.makeText(ctx, noProjectsMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SettingsStateContent(
        uiState = uiState,
        defaultWorkType = defaultWorkType,
        createActions = { successState ->
            createSettingsActions(
                settingsViewModel = settingsViewModel,
                successState = successState
            )
        }
    )
}

@Composable
internal fun SettingsStateContent(
    uiState: SettingsUiState,
    defaultWorkType: String,
    createActions: (SettingsUiState.Success) -> SettingsActions
) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = uiState is SettingsUiState.Loading
    )
    var lastSuccessState by remember { mutableStateOf<SettingsUiState.Success?>(value = null) }

    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.Success) {
            lastSuccessState = uiState
        }
    }

    when (uiState) {
        is SettingsUiState.Loading -> SettingsLoadingContent(
            showLoadingIndicator = showLoadingIndicator,
            lastSuccessState = lastSuccessState,
            defaultWorkType = defaultWorkType,
            createActions = createActions
        )
        is SettingsUiState.Success -> {
            val actions = remember(uiState) { createActions(uiState) }
            SettingsContent(
                uiState = uiState,
                actions = actions,
                defaultWorkType = defaultWorkType
            )
        }
        is SettingsUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(all = FORM_SECTION_SPACING),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.error_message, uiState.message),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun SettingsLoadingContent(
    showLoadingIndicator: Boolean,
    lastSuccessState: SettingsUiState.Success?,
    defaultWorkType: String,
    createActions: (SettingsUiState.Success) -> SettingsActions
) {
    if (showLoadingIndicator) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = FORM_SECTION_SPACING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (lastSuccessState != null) {
        val actions = remember(lastSuccessState) { createActions(lastSuccessState) }
        SettingsContent(uiState = lastSuccessState, actions = actions, defaultWorkType = defaultWorkType)
    } else {
        Box(modifier = Modifier.fillMaxSize())
    }
}

private fun createSettingsActions(
    settingsViewModel: SettingsViewModel,
    successState: SettingsUiState.Success
): SettingsActions {
    return SettingsActions(
        onNameChange = settingsViewModel::setName,
        onEmployerChange = settingsViewModel::setEmployer,
        onDailyWorkTimeEstimateChange = settingsViewModel::setDailyWorkTimeEstimate,
        onDailyLunchTimeEstimateChange = settingsViewModel::setDailyLunchTimeEstimate,
        onInitialFlexTimeTotalChange = settingsViewModel::setInitialFlexTimeTotal,
        onWorkTypeAdded = settingsViewModel::addWorkType,
        onWorkTypeRemoved = settingsViewModel::removeWorkType,
        onSave = { settingsViewModel.saveSettings() },
        onGeneratePdf = {
            settingsViewModel.requestMonthlyReport(
                name = successState.data.name,
                employer = successState.data.employer
            )
        }
    )
}

@Composable
internal fun SettingsContent(
    uiState: SettingsUiState.Success,
    actions: SettingsActions,
    defaultWorkType: String
) {
    val showDailyWorkTimePickerDialogState = remember { mutableStateOf(value = false) }
    val showDailyLunchTimeEstimatePickerDialogState = remember { mutableStateOf(value = false) }
    val workTypeUiState = rememberWorkTypeUiState(
        workTypes = uiState.data.workTypes,
        defaultWorkType = defaultWorkType,
        onWorkTypeRemoved = actions.onWorkTypeRemoved,
        onWorkTypeAdded = actions.onWorkTypeAdded
    )
    val saveUi = rememberSettingsSaveUi(
        data = uiState.data,
        selectedDate = uiState.selectedDate,
        onSave = actions.onSave
    )

    TimePickerDialogsSection(
        workTimePicker = TimePickerDialogConfig(
            time = uiState.data.dailyWorkTimeEstimate,
            isVisible = showDailyWorkTimePickerDialogState.value,
            onDismiss = { showDailyWorkTimePickerDialogState.value = false },
            onConfirm = {
                actions.onDailyWorkTimeEstimateChange(it)
                showDailyWorkTimePickerDialogState.value = false
            }
        ),
        lunchTimePicker = TimePickerDialogConfig(
            time = uiState.data.dailyLunchTimeEstimate,
            isVisible = showDailyLunchTimeEstimatePickerDialogState.value,
            onDismiss = { showDailyLunchTimeEstimatePickerDialogState.value = false },
            onConfirm = {
                actions.onDailyLunchTimeEstimateChange(it)
                showDailyLunchTimeEstimatePickerDialogState.value = false
            }
        )
    )

    SettingsContentBody(
        state = SettingsContentBodyState(
            uiState = uiState,
            actions = actions,
            workTypeState = workTypeUiState.dialogState,
            timePickerState = TimePickerState(
                onDailyWorkTimePickerClick = { showDailyWorkTimePickerDialogState.value = true },
                onDailyLunchTimeEstimatePickerClick = { showDailyLunchTimeEstimatePickerDialogState.value = true }
            ),
            addWorkTypeDialogState = workTypeUiState.addDialogState,
            scrollState = rememberScrollState(),
            saveUi = saveUi,
            defaultWorkType = defaultWorkType
        )
    )
}

@Composable
private fun rememberWorkTypeUiState(
    workTypes: List<String>,
    defaultWorkType: String,
    onWorkTypeRemoved: (String) -> Unit,
    onWorkTypeAdded: (String) -> Unit
): WorkTypeUiState {
    val context = LocalContext.current
    val protectedMessage = stringResource(id = R.string.default_work_type_cannot_be_deleted)
    val showAddDialogState = remember { mutableStateOf(value = false) }
    val selectedState = remember(defaultWorkType) { mutableStateOf(value = defaultWorkType) }

    LaunchedEffect(workTypes, defaultWorkType, selectedState.value) {
        if (selectedState.value.isBlank() || selectedState.value !in workTypes) {
            selectedState.value = defaultWorkType
        }
    }

    return WorkTypeUiState(
        dialogState = WorkTypeDialogState(
            selectedWorkType = selectedState.value,
            onWorkTypeSelected = { selectedState.value = it },
            onAddClick = { showAddDialogState.value = true },
            onDeleteClick = {
                if (selectedState.value != defaultWorkType) {
                    onWorkTypeRemoved(selectedState.value)
                    selectedState.value = defaultWorkType
                } else {
                    Toast.makeText(context, protectedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        ),
        addDialogState = AddWorkTypeDialogState(
            isVisible = showAddDialogState.value,
            onDismiss = { showAddDialogState.value = false },
            onConfirm = {
                onWorkTypeAdded(it)
                selectedState.value = it
                showAddDialogState.value = false
            }
        )
    )
}

@Composable
private fun SettingsContentBody(
    state: SettingsContentBodyState
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollbar(scrollState = state.scrollState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = state.scrollState)
                .padding(all = FORM_SECTION_SPACING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = SCREEN_CONTENT_SPACING)
        ) {
            HeaderSection(date = state.uiState.selectedDate)

            SettingsCard {
                ProfileSection(
                    name = state.uiState.data.name,
                    employer = state.uiState.data.employer,
                    onNameChange = state.actions.onNameChange,
                    onEmployerChange = state.actions.onEmployerChange
                )
            }

            GlobalDefaultsCard(state = state)

            SettingsCard {
                WorkTypeSection(
                    state = WorkTypeSectionState(
                        workTypes = state.uiState.data.workTypes,
                        dialogState = state.workTypeState,
                        protectedWorkType = state.defaultWorkType
                    )
                )
            }

            ActionButtonsSection(
                onSave = state.saveUi.onSaveRequested,
                onGeneratePdf = state.actions.onGeneratePdf,
                isPdfEnabled = state.uiState.selectedDate.isNotBlank(),
                isSaveEnabled = state.saveUi.isSaveEnabled
            )

            AddWorkTypeDialogSection(
                isVisible = state.addWorkTypeDialogState.isVisible,
                onDismiss = state.addWorkTypeDialogState.onDismiss,
                onConfirmed = state.addWorkTypeDialogState.onConfirm
            )
        }
    }
}

@Composable
private fun GlobalDefaultsCard(state: SettingsContentBodyState) {
    SettingsCard {
        Text(
            text = stringResource(id = R.string.global_defaults),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        HorizontalDivider()

        DailyWorkTimePickerRow(
            dailyWorkTime = state.uiState.data.dailyWorkTimeEstimate,
            onPickerClick = state.timePickerState.onDailyWorkTimePickerClick
        )

        DailyLunchTimeEstimatePickerRow(
            dailyLunchTimeEstimate = state.uiState.data.dailyLunchTimeEstimate,
            onPickerClick = state.timePickerState.onDailyLunchTimeEstimatePickerClick
        )

        SettingsTextField(
            value = state.uiState.data.initialFlexTimeTotal,
            label = R.string.initial_flex_time_total,
            onValueChange = state.actions.onInitialFlexTimeTotalChange,
            isError = state.saveUi.isInitialFlexTimeTotalError
        )
    }
}

@Composable
private fun TimePickerDialogsSection(
    workTimePicker: TimePickerDialogConfig,
    lunchTimePicker: TimePickerDialogConfig
) {
    DailyWorkTimePickerDialogSection(
        isVisible = workTimePicker.isVisible,
        currentDailyWorkTime = workTimePicker.time,
        onDismiss = workTimePicker.onDismiss,
        onConfirmed = workTimePicker.onConfirm
    )
    DailyLunchTimeEstimatePickerDialogSection(
        isVisible = lunchTimePicker.isVisible,
        currentDailyLunchTimeEstimate = lunchTimePicker.time,
        onDismiss = lunchTimePicker.onDismiss,
        onConfirmed = lunchTimePicker.onConfirm
    )
}

@Composable
private fun DailyLunchTimeEstimatePickerDialogSection(
    isVisible: Boolean,
    currentDailyLunchTimeEstimate: String,
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    if (isVisible) {
        TimePickerDialog(
            onDismissRequest = onDismiss,
            onConfirmation = onConfirmed,
            titleId = R.string.daily_lunch_time_estimate,
            time = currentDailyLunchTimeEstimate
        )
    }
}

@Composable
private fun DailyWorkTimePickerDialogSection(
    isVisible: Boolean,
    currentDailyWorkTime: String,
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    if (isVisible) {
        TimePickerDialog(
            onDismissRequest = onDismiss,
            onConfirmation = onConfirmed,
            titleId = R.string.daily_work_time,
            time = currentDailyWorkTime
        )
    }
}

@Composable
private fun AddWorkTypeDialogSection(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    if (isVisible) {
        AddTextFieldDialog(
            onDismissRequest = onDismiss,
            onConfirmation = onConfirmed,
            label = stringResource(id = R.string.work_type)
        )
    }
}

@Composable
private fun DailyWorkTimePickerRow(
    dailyWorkTime: String,
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
            singleLine = true,
            label = {
                Text(
                    text = stringResource(id = R.string.daily_work_time),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            modifier = Modifier.weight(weight = 1f),
            shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
        IconButton(onClick = onPickerClick) {
            Icon(imageVector = Icons.Default.AccessTime, contentDescription = null)
        }
    }
}

@Composable
private fun DailyLunchTimeEstimatePickerRow(
    dailyLunchTimeEstimate: String,
    onPickerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)
    ) {
        OutlinedTextField(
            value = dailyLunchTimeEstimate,
            onValueChange = {},
            enabled = false,
            singleLine = true,
            label = {
                Text(
                    text = stringResource(id = R.string.daily_lunch_time_estimate),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            modifier = Modifier.weight(weight = 1f),
            shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
        IconButton(onClick = onPickerClick) {
            Icon(imageVector = Icons.Default.AccessTime, contentDescription = null)
        }
    }
}

@Composable
private fun rememberSettingsSaveUi(
    data: SettingsState,
    selectedDate: String,
    onSave: () -> Unit
): SettingsSaveUi {
    val context = LocalContext.current
    val savedText = stringResource(id = R.string.saved)
    val lastSavedNameState = remember(selectedDate) { mutableStateOf(value = data.name) }
    val lastSavedEmployerState = remember(selectedDate) { mutableStateOf(value = data.employer) }
    val lastSavedDailyWorkTimeEstimateState = remember(selectedDate) {
        mutableStateOf(value = data.dailyWorkTimeEstimate)
    }
    val lastSavedDailyLunchTimeEstimateState = remember(selectedDate) {
        mutableStateOf(value = data.dailyLunchTimeEstimate)
    }
    val lastSavedInitialFlexTimeTotalState = remember(selectedDate) {
        mutableStateOf(value = data.initialFlexTimeTotal)
    }
    val lastSavedWorkTypesState = remember(selectedDate) { mutableStateOf(value = data.workTypes) }

    val hasUnsavedChanges =
        hasChanges(current = data.name, baseline = lastSavedNameState.value) ||
            hasChanges(current = data.employer, baseline = lastSavedEmployerState.value) ||
            hasChanges(
                current = data.dailyWorkTimeEstimate,
                baseline = lastSavedDailyWorkTimeEstimateState.value
            ) ||
            hasChanges(
                current = data.dailyLunchTimeEstimate,
                baseline = lastSavedDailyLunchTimeEstimateState.value
            ) ||
            hasChanges(
                current = data.initialFlexTimeTotal,
                baseline = lastSavedInitialFlexTimeTotalState.value
            ) ||
            hasChanges(current = data.workTypes, baseline = lastSavedWorkTypesState.value)
    val isInitialFlexTimeTotalError = remember(data.initialFlexTimeTotal) {
        !data.initialFlexTimeTotal.matches(INITIAL_FLEX_TIME_TOTAL_INPUT_REGEX)
    }

    return SettingsSaveUi(
        isSaveEnabled = isActionEnabled(
            hasRequiredFields = !isInitialFlexTimeTotalError,
            hasUnsavedChanges = hasUnsavedChanges
        ),
        isInitialFlexTimeTotalError = isInitialFlexTimeTotalError,
        onSaveRequested = {
            if (hasUnsavedChanges) {
                onSave()
                lastSavedNameState.value = data.name
                lastSavedEmployerState.value = data.employer
                lastSavedDailyWorkTimeEstimateState.value = data.dailyWorkTimeEstimate
                lastSavedDailyLunchTimeEstimateState.value = data.dailyLunchTimeEstimate
                lastSavedInitialFlexTimeTotalState.value = data.initialFlexTimeTotal
                lastSavedWorkTypesState.value = data.workTypes
                Toast.makeText(context, savedText, Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@Composable
private fun SettingsCard(title: String? = null, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().widthIn(max = FORM_MAX_WIDTH),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(all = FORM_SECTION_SPACING),
            verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
            }
            content()
        }
    }
}

@Composable
private fun HeaderSection(date: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = FORM_MAX_WIDTH),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(all = HEADER_CONTENT_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = HEADER_CONTENT_SPACING)
        ) {
            Header(title = stringResource(id = R.string.settings))
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ProfileSection(
    name: String,
    employer: String,
    onNameChange: (String) -> Unit,
    onEmployerChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)) {
        SettingsTextField(value = name, label = R.string.name, onValueChange = onNameChange)
        SettingsTextField(value = employer, label = R.string.employer, onValueChange = onEmployerChange)
    }
}

@Composable
private fun SettingsTextField(
    value: String,
    label: Int,
    onValueChange: (String) -> Unit,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        isError = isError,
        label = {
            Text(
                text = stringResource(id = label),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
    )
}

@Composable
private fun WorkTypeSection(state: WorkTypeSectionState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)
    ) {
        DropdownMenuBox(
            items = state.workTypes,
            onItemSelected = state.dialogState.onWorkTypeSelected,
            selectedText = state.dialogState.selectedWorkType,
            labelId = R.string.work_type,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)) {
            Button(
                onClick = state.dialogState.onAddClick,
                modifier = Modifier.weight(weight = 1f),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.add))
            }
            Button(
                onClick = state.dialogState.onDeleteClick,
                enabled = state.dialogState.selectedWorkType.isNotEmpty() &&
                    state.dialogState.selectedWorkType != state.protectedWorkType,
                modifier = Modifier.weight(weight = 1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.delete))
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onSave: () -> Unit,
    onGeneratePdf: () -> Unit,
    isPdfEnabled: Boolean,
    isSaveEnabled: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth().widthIn(max = FORM_MAX_WIDTH),
        verticalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)
    ) {
        Button(
            onClick = onSave,
            enabled = isSaveEnabled,
            modifier = Modifier.fillMaxWidth(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.save), fontSize = ACTION_BUTTON_FONT_SIZE)
        }
        Button(
            onClick = onGeneratePdf,
            enabled = isPdfEnabled,
            modifier = Modifier.fillMaxWidth(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.generate_pdf), fontSize = ACTION_BUTTON_FONT_SIZE)
        }
        Text(
            text = stringResource(id = R.string.monthly_help),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
        )
    }
}
data class SettingsActions(
    val onNameChange: (String) -> Unit,
    val onEmployerChange: (String) -> Unit,
    val onDailyWorkTimeEstimateChange: (String) -> Unit,
    val onDailyLunchTimeEstimateChange: (String) -> Unit,
    val onInitialFlexTimeTotalChange: (String) -> Unit,
    val onWorkTypeAdded: (String) -> Unit,
    val onWorkTypeRemoved: (String) -> Unit,
    val onSave: () -> Unit,
    val onGeneratePdf: () -> Unit
)

private data class WorkTypeDialogState(
    val selectedWorkType: String,
    val onWorkTypeSelected: (String) -> Unit,
    val onAddClick: () -> Unit,
    val onDeleteClick: () -> Unit
)

private data class WorkTypeSectionState(
    val workTypes: List<String>,
    val dialogState: WorkTypeDialogState,
    val protectedWorkType: String
)

private data class AddWorkTypeDialogState(
    val isVisible: Boolean,
    val onDismiss: () -> Unit,
    val onConfirm: (String) -> Unit
)

private data class TimePickerState(
    val onDailyWorkTimePickerClick: () -> Unit,
    val onDailyLunchTimeEstimatePickerClick: () -> Unit
)

private data class SettingsContentBodyState(
    val uiState: SettingsUiState.Success,
    val actions: SettingsActions,
    val workTypeState: WorkTypeDialogState,
    val timePickerState: TimePickerState,
    val addWorkTypeDialogState: AddWorkTypeDialogState,
    val scrollState: androidx.compose.foundation.ScrollState,
    val saveUi: SettingsSaveUi,
    val defaultWorkType: String
)

private data class WorkTypeUiState(
    val dialogState: WorkTypeDialogState,
    val addDialogState: AddWorkTypeDialogState
)

private data class TimePickerDialogConfig(
    val time: String,
    val isVisible: Boolean,
    val onDismiss: () -> Unit,
    val onConfirm: (String) -> Unit
)

private data class SettingsSaveUi(
    val isSaveEnabled: Boolean,
    val isInitialFlexTimeTotalError: Boolean,
    val onSaveRequested: () -> Unit
)

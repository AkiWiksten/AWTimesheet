@file:Suppress("TooManyFunctions")

package com.akiwiksten.worktime30.feature.settings

import android.content.Context
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
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ACTION_BUTTON_FONT_SIZE
import com.akiwiksten.worktime30.core.FIELD_CORNER_RADIUS
import com.akiwiksten.worktime30.core.FORM_GROUP_SPACING
import com.akiwiksten.worktime30.core.FORM_INLINE_SPACING
import com.akiwiksten.worktime30.core.FORM_MAX_WIDTH
import com.akiwiksten.worktime30.core.FORM_SECTION_SPACING
import com.akiwiksten.worktime30.core.HEADER_CONTENT_PADDING
import com.akiwiksten.worktime30.core.HEADER_CONTENT_SPACING
import com.akiwiksten.worktime30.core.LABEL_FONT_SIZE_SCALE
import com.akiwiksten.worktime30.core.SCREEN_CONTENT_SPACING
import com.akiwiksten.worktime30.core.ui.AddTextFieldDialog
import com.akiwiksten.worktime30.core.ui.DropdownMenuBox
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.TimePickerDialog
import com.akiwiksten.worktime30.core.ui.hasChanges
import com.akiwiksten.worktime30.core.ui.isActionEnabled
import com.akiwiksten.worktime30.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.worktime30.core.ui.verticalScrollbar
import com.akiwiksten.worktime30.domain.model.SettingsState

private val INITIAL_FLEX_TIME_TOTAL_INPUT_REGEX = Regex(pattern = "[+-]?(?:[1-9][0-9]+|0[0-9]):[0-5][0-9]")

@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val ctx = LocalContext.current

    LaunchedEffect(Unit) {
        settingsViewModel.loadSettings()
    }

    SettingsStateContent(
        uiState = uiState,
        createActions = { successState ->
            createSettingsActions(
                settingsViewModel = settingsViewModel,
                successState = successState,
                ctx = ctx
            )
        }
    )
}

@Composable
internal fun SettingsStateContent(
    uiState: SettingsUiState,
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
        is SettingsUiState.Loading -> {
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
            } else {
                val cachedState = lastSuccessState
                if (cachedState != null) {
                    val actions = remember(cachedState) { createActions(cachedState) }
                    SettingsContent(uiState = cachedState, actions = actions)
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
        is SettingsUiState.Success -> {
            val actions = remember(uiState) { createActions(uiState) }
            SettingsContent(uiState = uiState, actions = actions)
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
                    text = "Error: ${uiState.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

private fun createSettingsActions(
    settingsViewModel: SettingsViewModel,
    successState: SettingsUiState.Success,
    ctx: Context
): SettingsActions {
    return SettingsActions(
        onNameChange = settingsViewModel::setName,
        onEmployerChange = settingsViewModel::setEmployer,
        onDailyWorkTimeEstimateChange = settingsViewModel::setDailyWorkTimeEstimate,
        onDailyLunchTimeEstimateChange = settingsViewModel::setLunchTimeEstimate,
        onInitialFlexTimeTotalChange = settingsViewModel::setInitialFlexTimeTotal,
        onWorkTypeAdded = settingsViewModel::addWorkType,
        onWorkTypeRemoved = settingsViewModel::removeWorkType,
        onSave = { settingsViewModel.saveSettings() },
        onGeneratePdf = {
            generateReport(
                ctx = ctx,
                projectsByMonth = successState.projectsByMonth,
                endOfMonthDate = successState.endMonthDate,
                name = successState.data.name,
                employer = successState.data.employer
            )
        }
    )
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
    val saveUi: SettingsSaveUi
)

@Suppress("LongMethod")
@Composable
internal fun SettingsContent(
    uiState: SettingsUiState.Success,
    actions: SettingsActions
) {
    val showAddWorkTypeDialogState = remember { mutableStateOf(value = false) }
    val showDailyWorkTimePickerDialogState = remember { mutableStateOf(value = false) }
    val showDailyLunchTimeEstimatePickerDialogState = remember { mutableStateOf(value = false) }
    val selectedWorkTypeState = remember { mutableStateOf(value = "") }
    val saveUi = rememberSettingsSaveUi(
        data = uiState.data,
        selectedDate = uiState.selectedDate,
        onSave = actions.onSave
    )
    val scrollState = rememberScrollState()

    DailyWorkTimePickerDialogSection(
        isVisible = showDailyWorkTimePickerDialogState.value,
        currentDailyWorkTime = uiState.data.dailyWorkTimeEstimate,
        onDismiss = { showDailyWorkTimePickerDialogState.value = false },
        onConfirmed = { selectedTime ->
            actions.onDailyWorkTimeEstimateChange(selectedTime)
            showDailyWorkTimePickerDialogState.value = false
        }
    )

    DailyLunchTimeEstimatePickerDialogSection(
        isVisible = showDailyLunchTimeEstimatePickerDialogState.value,
        currentDailyLunchTimeEstimate = uiState.data.dailyLunchTimeEstimate,
        onDismiss = { showDailyLunchTimeEstimatePickerDialogState.value = false },
        onConfirmed = { selectedTime ->
            actions.onDailyLunchTimeEstimateChange(selectedTime)
            showDailyLunchTimeEstimatePickerDialogState.value = false
        }
    )

    SettingsContentBody(
        state = SettingsContentBodyState(
            uiState = uiState,
            actions = actions,
            workTypeState = WorkTypeDialogState(
                selectedWorkType = selectedWorkTypeState.value,
                onWorkTypeSelected = { selectedWorkTypeState.value = it },
                onAddClick = { showAddWorkTypeDialogState.value = true },
                onDeleteClick = {
                    actions.onWorkTypeRemoved(selectedWorkTypeState.value)
                    // Reset selection will be handled by LaunchedEffect(uiState.data.workTypes)
                }
            ),
            timePickerState = TimePickerState(
                onDailyWorkTimePickerClick = { showDailyWorkTimePickerDialogState.value = true },
                onDailyLunchTimeEstimatePickerClick = {
                    showDailyLunchTimeEstimatePickerDialogState.value = true
                }
            ),
            addWorkTypeDialogState = AddWorkTypeDialogState(
                isVisible = showAddWorkTypeDialogState.value,
                onDismiss = { showAddWorkTypeDialogState.value = false },
                onConfirm = {
                    actions.onWorkTypeAdded(it)
                    showAddWorkTypeDialogState.value = false
                }
            ),
            scrollState = scrollState,
            saveUi = saveUi
        )
    )
}

@Composable
private fun SettingsContentBody(
    state: SettingsContentBodyState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScrollbar(scrollState = state.scrollState)
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
                workTypes = state.uiState.data.workTypes,
                selectedWorkType = state.workTypeState.selectedWorkType,
                onWorkTypeSelected = state.workTypeState.onWorkTypeSelected,
                onAddClick = state.workTypeState.onAddClick,
                onDeleteClick = state.workTypeState.onDeleteClick
            )
        }

        ActionButtonsSection(
            onSave = state.saveUi.onSaveRequested,
            onGeneratePdf = state.actions.onGeneratePdf,
            isPdfEnabled = state.uiState.projectsByMonth.isNotEmpty(),
            isSaveEnabled = state.saveUi.isSaveEnabled
        )

        AddWorkTypeDialogSection(
            isVisible = state.addWorkTypeDialogState.isVisible,
            onDismiss = state.addWorkTypeDialogState.onDismiss,
            onConfirmed = state.addWorkTypeDialogState.onConfirm
        )
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

private data class SettingsSaveUi(
    val isSaveEnabled: Boolean,
    val isInitialFlexTimeTotalError: Boolean,
    val onSaveRequested: () -> Unit
)

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
private fun WorkTypeSection(
    workTypes: List<String>,
    selectedWorkType: String,
    onWorkTypeSelected: (String) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)
    ) {
        DropdownMenuBox(
            items = workTypes,
            onItemSelected = onWorkTypeSelected,
            selectedText = selectedWorkType,
            labelId = R.string.work_type,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)) {
            Button(
                onClick = onAddClick,
                modifier = Modifier.weight(weight = 1f),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.add))
            }
            Button(
                onClick = onDeleteClick,
                enabled = selectedWorkType.isNotEmpty(),
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

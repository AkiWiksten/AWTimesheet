package com.akiwiksten.worktime30.feature.projects.single

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
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
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.TimePickerDialog
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState

@Composable
fun HeaderSection(date: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            text = date,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleProjectTopBar(onNavigateBack: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
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
fun ProjectTimePickerDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit,
    currentTime: String
) {
    if (showDialog) {
        TimePickerDialog(
            onDismissRequest = onDismissRequest,
            onConfirmation = onConfirmation,
            titleId = R.string.project_time,
            time = currentTime
        )
    }
}

@Composable
internal fun DialogMainFields(
    state: SingleProjectState,
    isAddMode: Boolean,
    onStateChange: (SingleProjectState) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
        state.projectName.let {
            OutlinedTextField(
                value = it,
                onValueChange = { onStateChange(state.copy(projectName = it)) },
                label = { Text(text = stringResource(id = R.string.project_name)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isAddMode,
                singleLine = true,
                shape = RoundedCornerShape(size = 12.dp)
            )
        }

        state.kilometres.let {
            OutlinedTextField(
                value = it,
                onValueChange = { if (it.isDigitsOnly()) onStateChange(state.copy(kilometres = it)) },
                label = { Text(text = stringResource(id = R.string.kilometres)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(size = 12.dp)
            )
        }
    }
}

@Composable
private fun ProjectTimeSelectionRow(
    state: SingleProjectState,
    onOpenProjectDetails: () -> Unit,
    onOpenTimePicker: () -> Unit,
    onStateChange: (SingleProjectState) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = state.projectTime,
            onValueChange = { onStateChange(state.copy(projectTime = it)) },
            label = { Text(text = stringResource(id = R.string.project_time)) },
            modifier = Modifier.weight(weight = 1f),
            readOnly = true,
            leadingIcon = { Icon(imageVector = Icons.Default.AccessTime, contentDescription = null) },
            shape = RoundedCornerShape(size = 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Button(
            onClick = onOpenProjectDetails,
            modifier = Modifier.padding(top = 8.dp),
            shape = RoundedCornerShape(size = 12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(imageVector = Icons.Default.History, contentDescription = null)
            Spacer(modifier = Modifier.width(width = 4.dp))
            Text(text = stringResource(id = R.string.details))
        }

        Button(
            onClick = onOpenTimePicker,
            modifier = Modifier.padding(top = 8.dp),
            shape = RoundedCornerShape(size = 12.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = stringResource(id = R.string.go_to_time_picker)
            )
        }
    }
}

@Composable
internal fun TimeSelectionSection(
    state: SingleProjectState,
    workTimeToday: String,
    onOpenProjectDetails: () -> Unit,
    onStateChange: (SingleProjectState) -> Unit
) {
    val openTimePickerDialogState = remember { mutableStateOf(false) }

    ProjectTimePickerDialog(
        showDialog = openTimePickerDialogState.value,
        onDismissRequest = { openTimePickerDialogState.value = false },
        onConfirmation = { time ->
            onStateChange(state.copy(projectTime = time))
            openTimePickerDialogState.value = false
        },
        currentTime = state.projectTime
    )

    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        Text(
            text = "${stringResource(id = R.string.work_time_today)}: $workTimeToday",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary
        )

        ProjectTimeSelectionRow(
            state = state,
            onOpenProjectDetails = onOpenProjectDetails,
            onOpenTimePicker = { openTimePickerDialogState.value = true },
            onStateChange = onStateChange
        )
    }
}

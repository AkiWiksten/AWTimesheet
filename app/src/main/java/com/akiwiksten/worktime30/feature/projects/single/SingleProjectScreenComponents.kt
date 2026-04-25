package com.akiwiksten.worktime30.feature.projects.single

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.FIELD_CORNER_RADIUS
import com.akiwiksten.worktime30.core.FORM_GROUP_PADDING
import com.akiwiksten.worktime30.core.FORM_GROUP_SPACING
import com.akiwiksten.worktime30.core.FORM_INLINE_SPACING
import com.akiwiksten.worktime30.core.FORM_SECTION_SPACING
import com.akiwiksten.worktime30.core.HEADER_CONTENT_PADDING
import com.akiwiksten.worktime30.core.HEADER_CONTENT_SPACING
import com.akiwiksten.worktime30.core.LABEL_FONT_SIZE_SCALE
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.TimePickerDialog
import com.akiwiksten.worktime30.feature.workday.SingleProjectState

@Composable
fun HeaderSection(date: String, workTimeToday: String) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = HEADER_CONTENT_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = HEADER_CONTENT_SPACING)
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
                text = "${stringResource(id = R.string.work_time_today)}: $workTimeToday",
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
    Column(verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)) {
        OutlinedTextField(
            value = state.projectName,
            onValueChange = { onStateChange(state.copy(projectName = it)) },
            label = {
                Text(
                    text = stringResource(id = R.string.project_name),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = isAddMode,
            singleLine = true,
            shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS)
        )

        OutlinedTextField(
            value = state.kilometres,
            onValueChange = { if (it.isDigitsOnly()) onStateChange(state.copy(kilometres = it)) },
            label = {
                Text(
                    text = stringResource(id = R.string.kilometres),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS)
        )
    }
}

@Composable
private fun ProjectTimeSelectionRow(
    state: SingleProjectState,
    onOpenProjectDetails: () -> Unit,
    onOpenTimePicker: () -> Unit,
    onStateChange: (SingleProjectState) -> Unit
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
                .padding(all = FORM_GROUP_PADDING),
            horizontalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProjectTimeReadOnlyField(
                projectTime = state.projectTime,
                onStateChange = { projectTime -> onStateChange(state.copy(projectTime = projectTime)) }
            )

            ProjectTimeActionsColumn(
                onOpenProjectDetails = onOpenProjectDetails,
                onOpenTimePicker = onOpenTimePicker
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
                text = stringResource(id = R.string.project_time),
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
    onOpenTimePicker: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)) {
        Button(
            onClick = onOpenProjectDetails,
            shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
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

        Button(
            onClick = onOpenTimePicker,
            modifier = Modifier.align(alignment = Alignment.CenterHorizontally),
            shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
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
internal fun TimeSelectionSection(
    state: SingleProjectState,
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

    ProjectTimeSelectionRow(
        state = state,
        onOpenProjectDetails = onOpenProjectDetails,
        onOpenTimePicker = { openTimePickerDialogState.value = true },
        onStateChange = onStateChange
    )
}

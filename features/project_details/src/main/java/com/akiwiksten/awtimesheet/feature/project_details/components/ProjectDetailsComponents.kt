package com.akiwiksten.awtimesheet.feature.project_details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.feature.project_details.R
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.FORM_INLINE_SPACING
import com.akiwiksten.awtimesheet.core.LABEL_FONT_SIZE_SCALE
import com.akiwiksten.awtimesheet.core.ui.Header
import com.akiwiksten.awtimesheet.core.ui.TimePickerDialog
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.feature.project_details.ProjectDetailsTimeRowLabels
import com.akiwiksten.awtimesheet.feature.project_details.ProjectDetailsUiState
import com.akiwiksten.awtimesheet.core.ui.UnsavedChangesDialog as CoreUnsavedChangesDialog

@Composable
internal fun ProjectDetailsTimeRow(
    textFieldValue: String,
    stringId: Int,
    currentTime: () -> Unit,
    onConfirmation: (time: String) -> Unit,
    labels: ProjectDetailsTimeRowLabels = ProjectDetailsTimeRowLabels(),
) {
    val currentTimeLabelId = labels.currentTimeLabelId
    val timePickerLabelId = labels.timePickerLabelId
    val openTimePickerDialog = remember { mutableStateOf(value = false) }

    AddTimePickerDialog(
        isOpen = openTimePickerDialog.value,
        textFieldValue = textFieldValue,
        stringId = stringId,
        onDismissRequest = { openTimePickerDialog.value = false },
        onConfirmation = { time ->
            onConfirmation(time)
            openTimePickerDialog.value = false
        }
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)
    ) {
        ReadOnlyTimeField(
            textFieldValue = textFieldValue,
            stringId = stringId,
            modifier = Modifier.weight(weight = 1f)
        )

        LabeledIconAction(
            labelId = currentTimeLabelId,
            onClick = currentTime,
            icon = {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )

        LabeledIconAction(
            labelId = timePickerLabelId,
            onClick = { openTimePickerDialog.value = true },
            icon = {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        )
    }
}

@Composable
private fun AddTimePickerDialog(
    isOpen: Boolean,
    textFieldValue: String,
    stringId: Int,
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit,
) {
    if (!isOpen) return

    TimePickerDialog(
        onDismissRequest = onDismissRequest,
        onConfirmation = onConfirmation,
        time = textFieldValue,
        titleId = stringId,
    )
}

@Composable
private fun ReadOnlyTimeField(
    textFieldValue: String,
    stringId: Int,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = textFieldValue,
        onValueChange = {},
        label = {
            Text(
                text = stringResource(id = stringId),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        readOnly = true,
        enabled = false,
        modifier = modifier,
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun LabeledIconAction(
    labelId: Int?,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 2.dp)
    ) {
        labelId?.let {
            Text(
                text = stringResource(id = it),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onClick) {
            icon()
        }
    }
}

@Composable
internal fun ProjectDetailsNameField(name: String) {
    OutlinedTextField(
        value = name,
        onValueChange = {},
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
        readOnly = true,
        enabled = false,
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
    )
}

@Composable
internal fun ProjectDetailsUnsavedChangesDialog(
    showState: MutableState<Boolean>,
    uiState: ProjectDetailsUiState,
    unsavedMessage: String,
    onNavigateBack: () -> Unit,
    onConfirm: (ProjectDetailsState, SettingsState) -> Unit,
) {
    if (!showState.value) return
    val successState = uiState as? ProjectDetailsUiState.Success
    CoreUnsavedChangesDialog(
        onDismiss = { showState.value = false },
        onDiscard = onNavigateBack,
        onSave = successState?.let {
            {
                onConfirm(
                    it.details,
                    it.settings.copy(dailyLunchTimeEstimate = it.details.lunchTimeEstimate)
                )
            }
        },
        dialogText = unsavedMessage
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProjectDetailsTopBar(onNavigateBack: () -> Unit) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        CenterAlignedTopAppBar(
            title = {
                Header(
                    title = stringResource(id = R.string.project_details),
                    modifier = Modifier.padding(top = 0.dp),
                    fillMaxWidth = false
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }
        )
    }
}

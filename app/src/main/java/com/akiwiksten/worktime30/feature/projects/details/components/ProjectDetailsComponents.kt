package com.akiwiksten.worktime30.feature.projects.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.core.FIELD_CORNER_RADIUS
import com.akiwiksten.worktime30.core.FORM_INLINE_SPACING
import com.akiwiksten.worktime30.core.LABEL_FONT_SIZE_SCALE
import com.akiwiksten.worktime30.core.ui.TimePickerDialog


fun isValidText(text: String): Boolean {
    return text.matches(regex = Regex(pattern = "-?[1-9][0-9]+:[0-5][0-9]")) ||
        text.matches(regex = Regex(pattern = "-?0[0-9]:[0-5][0-9]"))
}

@Composable
fun AddCustomTimeRow(
    customTime: String,
    customTimeFunction: (String, Boolean) -> Unit,
    stringId: Int
) {
    OutlinedTextField(
        value = customTime,
        onValueChange = { customTimeFunction(it, isValidText(text = it)) },
        singleLine = true,
        label = {
            Text(
                text = stringResource(id = stringId),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = false,
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        isError = !isValidText(text = customTime)
    )
}

@Composable
fun AddTimeRow(
    textFieldValue: String,
    stringId: Int,
    currentTime: () -> Unit,
    onConfirmation: (time: String) -> Unit,
    currentTimeLabelId: Int? = null,
    timePickerLabelId: Int? = null,
) {
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

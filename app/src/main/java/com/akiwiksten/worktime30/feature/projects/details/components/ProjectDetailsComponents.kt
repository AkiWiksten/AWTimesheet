package com.akiwiksten.worktime30.feature.projects.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        label = { Text(text = stringResource(id = stringId)) },
        modifier = Modifier.fillMaxWidth(),
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
) {
    val openTimePickerDialog = remember { mutableStateOf(value = false) }

    if (openTimePickerDialog.value) {
        TimePickerDialog(
            onDismissRequest = { openTimePickerDialog.value = false },
            onConfirmation = { time ->
                onConfirmation(time)
                openTimePickerDialog.value = false
            },
            time = textFieldValue,
            titleId = stringId,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {},
            label = { Text(text = stringResource(id = stringId)) },
            readOnly = true,
            enabled = false,
            modifier = Modifier.weight(weight = 1f),
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
            )
        )

        IconButton(onClick = currentTime) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = { openTimePickerDialog.value = true }) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

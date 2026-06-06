package com.akiwiksten.awtimesheet.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.R

@Composable
fun MyAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    titleAndText: Pair<String, String>,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(imageVector = icon, contentDescription = null)
        },
        title = {
            Text(text = titleAndText.first)
        },
        text = {
            Text(text = titleAndText.second)
        },
        confirmButton = {
            TextButton(onClick = onConfirmation) {
                Text(text = stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.dismiss))
            }
        }
    )
}

@Composable
fun AddTextFieldDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismissRequest) {
        var addText by remember { mutableStateOf(value = "") }
        val scrollState = rememberScrollState()

        Card(
            modifier = modifier
                .fillMaxWidth()
                .width(width = 280.dp)
                .height(intrinsicSize = IntrinsicSize.Min)
                .padding(all = PADDING_SPACING),
            shape = RoundedCornerShape(size = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(all = 24.dp)
                    .verticalScrollbar(scrollState = scrollState)
                    .verticalScroll(state = scrollState),
                verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = addText,
                    onValueChange = { addText = it },
                    label = { Text(text = label) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    singleLine = true,
                    isError = addText.isBlank()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(text = stringResource(id = R.string.dismiss))
                    }
                    TextButton(
                        onClick = { onConfirmation(addText) },
                        enabled = addText.isNotBlank()
                    ) {
                        Text(text = stringResource(id = R.string.confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun UnsavedChangesDialog(
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    dialogText: String,
    onSave: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
        title = { Text(text = stringResource(id = R.string.unsaved_data_title)) },
        text = { Text(text = dialogText) },
        confirmButton = {
            Row {
                if (onSave != null) {
                    TextButton(onClick = onSave) {
                        Text(text = stringResource(id = R.string.save))
                    }
                }
                TextButton(onClick = onDiscard) {
                    Text(text = stringResource(id = R.string.discard))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.stay))
            }
        }
    )
}

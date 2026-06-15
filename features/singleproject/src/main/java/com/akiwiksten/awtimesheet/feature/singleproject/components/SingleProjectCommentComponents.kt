package com.akiwiksten.awtimesheet.feature.singleproject.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.LABEL_FONT_SIZE_SCALE
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.ui.AwtButton
import com.akiwiksten.awtimesheet.feature.singleproject.R
import com.akiwiksten.awtimesheet.core.R as CoreR

@Composable
internal fun SingleProjectCommentField(
    comment: String,
    onCommentChange: (String) -> Unit
) {
    val showDialogState = rememberSaveable { mutableStateOf(false) }

    if (showDialogState.value) {
        CommentEditDialog(
            comment = comment,
            onConfirm = { newComment ->
                onCommentChange(newComment)
                showDialogState.value = false
            },
            onDismiss = { showDialogState.value = false }
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
    ) {
        OutlinedTextField(
            value = comment,
            onValueChange = onCommentChange,
            label = {
                Text(
                    text = stringResource(id = CoreR.string.comment),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            modifier = Modifier.weight(weight = 1f),
            singleLine = true,
            shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS)
        )
        IconButton(onClick = { showDialogState.value = true }) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(id = R.string.comment_dialog_title)
            )
        }
    }
}

@Composable
private fun CommentEditDialog(
    comment: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val editedComment = rememberSaveable(comment) { mutableStateOf(comment) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.comment_dialog_title)) },
        text = {
            OutlinedTextField(
                value = editedComment.value,
                onValueChange = { editedComment.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
                maxLines = 8,
            )
        },
        confirmButton = {
            AwtButton(onClick = { onConfirm(editedComment.value) }) {
                Text(text = stringResource(id = android.R.string.ok))
            }
        },
        dismissButton = {
            AwtButton(onClick = onDismiss) {
                Text(text = stringResource(id = android.R.string.cancel))
            }
        }
    )
}

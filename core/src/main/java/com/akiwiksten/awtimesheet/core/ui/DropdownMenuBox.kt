package com.akiwiksten.awtimesheet.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.toSize
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.LABEL_FONT_SIZE_SCALE
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuBox(
    items: List<String>,
    onItemSelected: (String) -> Unit,
    field: DropdownMenuField,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(value = false) }
    var textFieldSize by remember { mutableStateOf(value = Size.Zero) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = PADDING_SPACING_SMALL)
    ) {
        Box {
            DropdownTextField(
                selectedText = field.selectedText,
                expanded = expanded,
                labelId = field.labelId,
                enabled = field.enabled,
                onSizeChanged = { textFieldSize = it }
            )
            if (field.enabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(onClick = { expanded = !expanded })
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(width = with(LocalDensity.current) { textFieldSize.width.toDp() })
        ) {
            items.forEach { label ->
                DropdownMenuItem(
                    text = { Text(text = label, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onItemSelected(label)
                        expanded = false
                    }
                )
            }
        }
    }
}

data class DropdownMenuField(
    val labelId: Int,
    val selectedText: String = "",
    val enabled: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownTextField(
    selectedText: String,
    expanded: Boolean,
    labelId: Int,
    enabled: Boolean,
    onSizeChanged: (Size) -> Unit
) {
    OutlinedTextField(
        value = selectedText.trim(),
        onValueChange = { },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onSizeChanged(coordinates.size.toSize())
            },
        label = {
            Text(
                text = stringResource(id = labelId),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        },
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        isError = enabled && selectedText.trim().isEmpty() && labelId == R.string.allowance,
        enabled = enabled,
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
    )
}

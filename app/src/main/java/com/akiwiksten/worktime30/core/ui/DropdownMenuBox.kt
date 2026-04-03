package com.akiwiksten.worktime30.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.akiwiksten.worktime30.R

@Suppress("FunctionNaming")
@Composable
fun DropdownMenuBox(
    items: List<String>,
    selectedTextFunc: (String) -> Unit,
    stringId: Int,
    modifier: Modifier = Modifier,
    selectedText: String = "",
) {

    // Declaring a boolean value to store
    // the expanded state of the Text Field
    var expanded by remember { mutableStateOf(false) }

    var textFieldSize by remember { mutableStateOf(Size.Zero)}

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(10.dp).fillMaxWidth()
    ) {
        // Up Icon when expanded and down icon when collapsed
        val icon = if (expanded) {
            Icons.Filled.KeyboardArrowUp
        } else {
            Icons.Filled.KeyboardArrowDown
        }
        // Create an Outlined Text Field
        // with icon and not expanded
        OutlinedTextField(
            value = selectedText.trim(),
            onValueChange = {  },
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    // This value is used to assign to
                    // the DropDown the same width
                    textFieldSize = coordinates.size.toSize()
                },
            label = { Text(stringResource(stringId), fontSize = 20.sp) },
            trailingIcon = {
                Icon(icon,"contentDescription",
                    Modifier.clickable { expanded = !expanded })
            },
            textStyle = TextStyle.Default.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
            isError = selectedText.trim().isEmpty() && stringId == R.string.allowance,
            enabled = true
        )

        // Create a drop-down menu with list,
        // when clicked, set the Text Field text as the item selected
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current){textFieldSize.width.toDp()})
        ) {
            items.forEach { label ->
                DropdownMenuItem(
                    onClick = {
                        selectedTextFunc(label)
                        expanded = false
                    },
                    text = { Text(text = label) },
                )
            }
        }
    }
}

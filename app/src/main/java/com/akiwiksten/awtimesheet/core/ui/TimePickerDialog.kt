package com.akiwiksten.awtimesheet.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.core.TIME_FORMAT
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit,
    titleId: Int,
    time: String,
    modifier: Modifier = Modifier
) {
    val initialHour = time.substringBefore(delimiter = ':', missingDelimiterValue = "0").toIntOrNull() ?: 0
    val initialMinute = time.substringAfter(delimiter = ':', missingDelimiterValue = "0").toIntOrNull() ?: 0
    val scrollState = rememberScrollState()

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(all = 16.dp),
            shape = RoundedCornerShape(size = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(state = scrollState)
                    .padding(all = 32.dp),
                verticalArrangement = Arrangement.spacedBy(space = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = titleId),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TimePicker(
                    state = timePickerState,
                    layoutType = TimePickerLayoutType.Vertical,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismissRequest
                    ) {
                        Text(text = stringResource(id = R.string.dismiss))
                    }
                    TextButton(
                        onClick = {
                            onConfirmation(
                                formatTime(hour = timePickerState.hour, minute = timePickerState.minute)
                            )
                        }
                    ) {
                        Text(text = stringResource(id = R.string.confirm))
                    }
                }
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val formatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
    val time = LocalTime.of(hour, minute)
    return time.format(formatter)
}

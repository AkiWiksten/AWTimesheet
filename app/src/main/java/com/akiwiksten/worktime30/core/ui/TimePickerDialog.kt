package com.akiwiksten.worktime30.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.TIME_FORMAT
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("FunctionNaming")
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: (time: String) -> Unit,
    titleId: Int,
    time: String
) {
    Dialog(onDismissRequest = { onDismissRequest() }) {
        val timePickerState = rememberTimePickerState(
            initialHour = time.substringBefore(':').toInt(),
            initialMinute = time.substringAfter(':').toInt(),
            is24Hour = true
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(titleId),
                    fontSize = 15.sp,
                )
                TimePicker(
                    state = timePickerState,
                )

                TextButton(
                    onClick = { onDismissRequest() },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(stringResource(R.string.dismiss))
                }
                TextButton(
                    onClick = {
                        onConfirmation(
                            formatTime(
                                timePickerState.hour, timePickerState.minute
                            )
                        )
                    },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}

fun formatTime(hour : Int, minute : Int) : String {
    val formatter = DateTimeFormatter.ofPattern(TIME_FORMAT)
    val time = LocalDateTime.of(0, 1, 1, hour, minute)
    return time.format(formatter)
}

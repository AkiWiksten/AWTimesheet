package com.akiwiksten.worktime30.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.Header

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
fun CalendarScreen(
    calendarViewModel: CalendarViewModel = hiltViewModel(),
) {
    val uiState by calendarViewModel.uiState.collectAsState()
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            val selectedDate = calendarViewModel.convertMillisToDate(it)
            if (selectedDate != uiState.date) {
                calendarViewModel.onDateSelected(selectedDate)
            }
        }
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(5.dp)
        ) {
            Header(stringResource(R.string.select_work_day_date))

            Spacer(modifier = Modifier.padding(20.dp))

            OutlinedTextField(
                value = uiState.date,
                onValueChange = { },
                label = { Text(stringResource((R.string.selected_date)), fontSize = 20.sp) },
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = stringResource((R.string.select_date))
                    )
                },
                modifier = Modifier
                    .height(64.dp),
                textStyle = TextStyle.Default.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
            )

            Spacer(modifier = Modifier.padding(20.dp))

            Box(
                modifier = Modifier
                    .shadow(elevation = 4.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                DatePicker(
                    state = datePickerState,
                    showModeToggle = true,
                )
            }
        }
        Column(
            modifier = Modifier
                .padding(vertical = 20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                stringResource(R.string.selected_work_day),
                fontSize = 20.sp,
                modifier = Modifier.padding(5.dp)
            )
            Text(
                uiState.timePerDay,
                fontSize = 20.sp,
                modifier = Modifier.padding(5.dp),
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.selected_work_week),
                fontSize = 20.sp,
                modifier = Modifier.padding(5.dp)
            )
            Text(
                uiState.timePerWeek,
                fontSize = 20.sp,
                modifier = Modifier.padding(5.dp),
                fontWeight = FontWeight.Bold
            )
            Text(
                stringResource(R.string.selected_work_month),
                fontSize = 20.sp,
                modifier = Modifier.padding(5.dp)
            )
            Text(
                uiState.timePerMonth,
                fontSize = 20.sp,
                modifier = Modifier.padding(5.dp),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@file:Suppress("MagicNumber")

package com.akiwiksten.worktime30.feature.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.GeneratePdfParams
import com.akiwiksten.worktime30.core.MonthlyReportGenerator
import com.akiwiksten.worktime30.core.ui.AddTextFieldDialog
import com.akiwiksten.worktime30.core.ui.DropdownMenuBox
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.feature.calendar.CalendarViewModel

@Composable
@Suppress("LongMethod")
fun SettingsScreen(
    calendarViewModel: CalendarViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val name by settingsViewModel.name.collectAsState()
    val employer by settingsViewModel.employer.collectAsState()
    val calendarUiState by calendarViewModel.uiState.collectAsState()
    val date = calendarUiState.date
    val endMonthDate by settingsViewModel.endMonthDate.collectAsState()
    val dropDownWorkTypes by settingsViewModel.dropDownWorkTypes.collectAsState()
    val saveString = stringResource(R.string.saved)
    val ctx = LocalContext.current
    var openAddText by remember { mutableStateOf(false) }
    var selectedWorkType by remember { mutableStateOf("") }
    val projectTitles: List<String> = listOf(
        stringResource(R.string.date),
        stringResource(R.string.project),
        stringResource(R.string.start_time),
        stringResource(R.string.end_time),
        stringResource(R.string.work_time_today),
        stringResource(R.string.allowance),
        stringResource(R.string.work_type),
        stringResource(R.string.kilometres)
    )

    LaunchedEffect(date) {
        settingsViewModel.loadSettings()
        if (date.isNotEmpty()) {
            settingsViewModel.loadProjectsByMonth(date)
            settingsViewModel.setEndMonthDate(date)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val totalSumLabel = stringResource(R.string.total_sum)
        val monthlyReportLabel = stringResource(R.string.monthly_report)

        Header(stringResource(R.string.settings))

        Text(
            text = date,
            fontSize = 30.sp,
        )

        OutlinedTextField(
            value = name,
            onValueChange = {
                settingsViewModel.setName(it)
            },
            singleLine = true,
            label = { Text(stringResource(R.string.name), fontSize = 20.sp) },
            modifier = Modifier
                .width(500.dp)
                .padding(10.dp),
            textStyle = TextStyle.Default.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
        )

        OutlinedTextField(
            value = employer,
            onValueChange = {
                settingsViewModel.setEmployer(it)
            },
            singleLine = true,
            label = { Text(stringResource(R.string.employer), fontSize = 20.sp) },
            modifier = Modifier
                .width(500.dp)
                .padding(10.dp),
            textStyle = TextStyle.Default.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
        )

        Spacer(modifier = Modifier.padding(10.dp))
        HorizontalDivider(thickness = 2.dp)

        DropdownMenuBox(
            items = dropDownWorkTypes,
            onItemSelected = { selectedWorkType = it },
            selectedText = selectedWorkType,
            labelId = R.string.work_type,
            modifier = Modifier
                .width(500.dp),
        )

        Row {
            Button(
                onClick = {
                    openAddText = true
                },
                modifier = Modifier.padding(10.dp)
            ) { Text(modifier = Modifier.padding(6.dp), text = stringResource(R.string.add)) }
            Button(
                onClick = {
                    settingsViewModel.removeWorkType(selectedWorkType)
                    selectedWorkType = ""
                },
                modifier = Modifier.padding(10.dp),
                enabled = selectedWorkType.isNotEmpty()
            ) { Text(modifier = Modifier.padding(6.dp), text = stringResource(R.string.delete)) }
        }

        HorizontalDivider(thickness = 2.dp)

        if (openAddText) {
            AddTextFieldDialog(
                onDismissRequest = { openAddText = false },
                onConfirmation = { addText ->
                    settingsViewModel.addWorkType(addText)
                    openAddText = false
                },
                label = stringResource(R.string.work_type)
            )
        }

        Button(
            onClick = {
                settingsViewModel.saveSettings()
                Toast.makeText(ctx, saveString, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .width(500.dp)
                .padding(20.dp)
        ) {
            Text(
                modifier = Modifier.padding(6.dp),
                text = stringResource(R.string.save),
                fontSize = 20.sp
            )
        }

        Button(
            modifier = Modifier
                .width(500.dp)
                .padding(20.dp),
            onClick = {
                if (settingsViewModel.projectsByMonth.isNotEmpty()) {
                    MonthlyReportGenerator.generatePdf(
                        GeneratePdfParams(
                            ctx = ctx,
                            projectsByMonth = settingsViewModel.projectsByMonth,
                            endOfMonthDate = endMonthDate,
                            totalSumLabel = totalSumLabel,
                            monthlyReportLabel = monthlyReportLabel,
                            name = name,
                            employer = employer,
                            projectTitles = projectTitles
                        )
                    )
                }
            }
        ) {
            Text(
                modifier = Modifier.padding(6.dp),
                text = stringResource(R.string.generate_pdf),
                fontSize = 20.sp
            )
        }
        Text(
            text = stringResource(R.string.monthly_help),
            fontSize = 15.sp,
        )
    }
}

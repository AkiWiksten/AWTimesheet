package com.akiwiksten.worktime30.feature.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
fun SettingsScreen(
    calendarViewModel: CalendarViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val name by settingsViewModel.name.collectAsState()
    val employer by settingsViewModel.employer.collectAsState()
    val calendarUiState by calendarViewModel.uiState.collectAsState()
    val endMonthDate by settingsViewModel.endMonthDate.collectAsState()
    val dropDownWorkTypes by settingsViewModel.dropDownWorkTypes.collectAsState()
    
    val date = calendarUiState.date
    val ctx = LocalContext.current
    var showAddWorkTypeDialog by remember { mutableStateOf(false) }
    var selectedWorkType by remember { mutableStateOf("") }
    val savedMessage = stringResource(R.string.saved)

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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HeaderSection(date = date)

        ProfileSection(
            name = name,
            employer = employer,
            onNameChange = settingsViewModel::setName,
            onEmployerChange = settingsViewModel::setEmployer
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        WorkTypeSection(
            workTypes = dropDownWorkTypes,
            selectedWorkType = selectedWorkType,
            onWorkTypeSelected = { selectedWorkType = it },
            onAddClick = { showAddWorkTypeDialog = true },
            onDeleteClick = {
                settingsViewModel.removeWorkType(selectedWorkType)
                selectedWorkType = ""
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        ActionButtonsSection(
            onSave = {
                settingsViewModel.saveSettings()
                Toast.makeText(ctx, savedMessage, Toast.LENGTH_SHORT).show()
            },
            onGeneratePdf = {
                generateReport(
                    ctx = ctx,
                    viewModel = settingsViewModel,
                    endOfMonthDate = endMonthDate,
                    name = name,
                    employer = employer
                )
            },
            isPdfEnabled = settingsViewModel.projectsByMonth.isNotEmpty()
        )

        if (showAddWorkTypeDialog) {
            AddTextFieldDialog(
                onDismissRequest = { showAddWorkTypeDialog = false },
                onConfirmation = {
                    settingsViewModel.addWorkType(it)
                    showAddWorkTypeDialog = false
                },
                label = stringResource(R.string.work_type)
            )
        }
    }
}

@Composable
private fun HeaderSection(date: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Header(stringResource(R.string.settings))
        Text(text = date, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProfileSection(
    name: String,
    employer: String,
    onNameChange: (String) -> Unit,
    onEmployerChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsTextField(value = name, label = R.string.name, onValueChange = onNameChange)
        SettingsTextField(value = employer, label = R.string.employer, onValueChange = onEmployerChange)
    }
}

@Composable
private fun SettingsTextField(value: String, label: Int, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = { Text(stringResource(label)) },
        modifier = Modifier.fillMaxWidth(),
        textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold)
    )
}

@Composable
private fun WorkTypeSection(
    workTypes: List<String>,
    selectedWorkType: String,
    onWorkTypeSelected: (String) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DropdownMenuBox(
            items = workTypes,
            onItemSelected = onWorkTypeSelected,
            selectedText = selectedWorkType,
            labelId = R.string.work_type,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddClick, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.add))
            }
            Button(
                onClick = onDeleteClick,
                enabled = selectedWorkType.isNotEmpty(),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.delete))
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onSave: () -> Unit,
    onGeneratePdf: () -> Unit,
    isPdfEnabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.save), fontSize = 18.sp)
        }
        Button(
            onClick = onGeneratePdf,
            enabled = isPdfEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.generate_pdf), fontSize = 18.sp)
        }
        Text(
            text = stringResource(R.string.monthly_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

private fun generateReport(
    ctx: Context,
    viewModel: SettingsViewModel,
    endOfMonthDate: String,
    name: String,
    employer: String
) {
    val titles = listOf(
        R.string.date, R.string.project, R.string.start_time, R.string.end_time,
        R.string.work_time_today, R.string.allowance, R.string.work_type, R.string.kilometres
    ).map { ctx.getString(it) }

    MonthlyReportGenerator.generatePdf(
        GeneratePdfParams(
            ctx = ctx,
            projectsByMonth = viewModel.projectsByMonth,
            endOfMonthDate = endOfMonthDate,
            totalSumLabel = ctx.getString(R.string.total_sum),
            monthlyReportLabel = ctx.getString(R.string.monthly_report),
            name = name,
            employer = employer,
            projectTitles = titles
        )
    )
}

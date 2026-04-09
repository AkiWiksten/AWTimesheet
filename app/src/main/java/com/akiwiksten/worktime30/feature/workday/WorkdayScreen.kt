package com.akiwiksten.worktime30.feature.workday

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.feature.calendar.CalendarUiState
import com.akiwiksten.worktime30.feature.calendar.CalendarViewModel
import com.akiwiksten.worktime30.feature.workday.components.ExistingDayFields
import com.akiwiksten.worktime30.feature.workday.components.FooterSection
import com.akiwiksten.worktime30.feature.workday.components.HeaderSection
import com.akiwiksten.worktime30.feature.workday.components.NewDayFields
import com.akiwiksten.worktime30.feature.workday.components.ProjectNameField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkdayScreen(
    args: WorkdayArgs,
    onNavigateBack: () -> Unit,
    onConfirm: (WorkdayEntity, WorkStatsEntity) -> Unit,
    viewModel: WorkdayViewModel = hiltViewModel(),
) {
    val calendarViewModel: CalendarViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val calendarUiState by calendarViewModel.uiState.collectAsState()
    val currentCalendarState = calendarUiState  // Store in local variable for smart cast
    val date = (currentCalendarState as? CalendarUiState.Success)?.date ?: ""

    BackHandler(onBack = onNavigateBack)

    LaunchedEffect(Unit) {
        viewModel.setDate(date0 = date)
        args.projectName?.let { viewModel.setProjectName(projectName = it) }
        viewModel.loadWorkday(workdayArg = args.workday, workStatsArg = args.workStats)
    }

    Scaffold(
        topBar = {
            WorkdayTopBar(onNavigateBack = onNavigateBack)
        }
    ) { padding ->
        when (uiState) {
            is WorkdayUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(all = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is WorkdayUiState.Success -> {
                val successState = uiState as WorkdayUiState.Success
                WorkdayContent(
                    padding = padding,
                    uiState = successState,
                    projectName = args.projectName,
                    viewModel = viewModel,
                    onConfirm = {
                        val workdayResult = viewModel.getWorkdayEntity()
                        val workStatsResult = viewModel.getWorkStatsEntity()
                        onConfirm(workdayResult, workStatsResult)
                    }
                )
            }
            is WorkdayUiState.Error -> {
                val errorState = uiState as WorkdayUiState.Error
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(all = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Error: ${errorState.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkdayTopBar(onNavigateBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = {
            Header(
                title = stringResource(id = R.string.work_day),
                modifier = Modifier.padding(top = 0.dp),
                fillMaxWidth = false
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }
    )
}

@Composable
private fun WorkdayContent(
    padding: PaddingValues,
    uiState: WorkdayUiState.Success,
    projectName: String?,
    viewModel: WorkdayViewModel,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = padding)
            .padding(all = 16.dp)
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(space = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderSection(date = uiState.date, onClearDay = viewModel::clearDay)

        projectName?.let {
            ProjectNameField(name = it)
        }

        if (uiState.isNewDay) {
            NewDayFields(uiState = uiState, viewModel = viewModel)
        } else {
            ExistingDayFields(uiState = uiState, viewModel = viewModel)
        }

        FooterSection(onConfirm = onConfirm)
    }
}

data class WorkdayArgs(
    val projectName: String? = null,
    val workday: WorkdayEntity? = null,
    val workStats: WorkStatsEntity? = null
)

package com.akiwiksten.worktime30.feature.editworkday

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.feature.calendar.CalendarViewModel
import com.akiwiksten.worktime30.feature.editworkday.components.ExistingDayFields
import com.akiwiksten.worktime30.feature.editworkday.components.FooterSection
import com.akiwiksten.worktime30.feature.editworkday.components.HeaderSection
import com.akiwiksten.worktime30.feature.editworkday.components.NewDayFields
import com.akiwiksten.worktime30.feature.editworkday.components.ProjectNameField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkDayScreen(
    args: EditWorkDayArgs,
    onNavigateBack: () -> Unit,
    onConfirm: (WorkDayEntity, WorkStatsEntity) -> Unit,
    viewModel: EditWorkDayViewModel = hiltViewModel(),
) {
    val calendarViewModel: CalendarViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(onBack = onNavigateBack)

    LaunchedEffect(Unit) {
        viewModel.setDate(date0 = calendarViewModel.uiState.value.date)
        args.projectName?.let { viewModel.setProjectName(projectName = it) }
        viewModel.loadWorkDay(workDayArg = args.workDay, workStatsArg = args.workStats)
    }

    Scaffold(
        topBar = {
            EditWorkDayTopBar(onNavigateBack = onNavigateBack)
        }
    ) { padding ->
        EditWorkDayContent(
            padding = padding,
            uiState = uiState,
            projectName = args.projectName,
            viewModel = viewModel,
            onConfirm = {
                val workDayResult = viewModel.getWorkDayEntity()
                val workStatsResult = viewModel.getWorkStatsEntity()
                onConfirm(workDayResult, workStatsResult)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditWorkDayTopBar(onNavigateBack: () -> Unit) {
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
private fun EditWorkDayContent(
    padding: PaddingValues,
    uiState: EditWorkDayUiState,
    projectName: String?,
    viewModel: EditWorkDayViewModel,
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

data class EditWorkDayArgs(
    val projectName: String? = null,
    val workDay: WorkDayEntity? = null,
    val workStats: WorkStatsEntity? = null
)

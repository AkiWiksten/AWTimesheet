package com.akiwiksten.worktime30.feature.workday

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ui.Header
import com.akiwiksten.worktime30.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.feature.workday.components.ExistingDayFields
import com.akiwiksten.worktime30.feature.workday.components.FooterSection
import com.akiwiksten.worktime30.feature.workday.components.HeaderSection
import com.akiwiksten.worktime30.feature.workday.components.NewDayFields
import com.akiwiksten.worktime30.feature.workday.components.ProjectNameField
import com.akiwiksten.worktime30.feature.workday.components.WorkdayFieldActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkdayScreen(
    args: WorkdayArgs,
    onNavigateBack: () -> Unit,
    onConfirm: (WorkdayEntity, WorkStatsEntity) -> Unit,
    viewModel: WorkdayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(onBack = onNavigateBack)

    LaunchedEffect(Unit) {
        args.projectName?.let { viewModel.setProjectName(projectName = it) }
        viewModel.loadWorkday(workdayArg = args.workday, workStatsArg = args.workStats)
    }

    Scaffold(
        topBar = {
            WorkdayTopBar(onNavigateBack = onNavigateBack)
        }
    ) { padding ->
        val actions = remember(viewModel, onConfirm) {
            createWorkdayScreenActions(viewModel = viewModel) {
                val workdayResult = viewModel.getWorkdayEntity()
                val workStatsResult = viewModel.getWorkStatsEntity()
                onConfirm(workdayResult, workStatsResult)
            }
        }

        WorkdayStateContent(
            padding = padding,
            uiState = uiState,
            projectName = args.projectName,
            actions = actions
        )
    }
}

@Composable
internal fun WorkdayLoadingState(padding: PaddingValues, showLoadingIndicator: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showLoadingIndicator) {
            CircularProgressIndicator()
        }
    }
}

@Composable
internal fun WorkdayErrorState(padding: PaddingValues, errorMessage: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Error: $errorMessage",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkdayTopBar(onNavigateBack: () -> Unit) {
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
internal fun WorkdayContent(
    padding: PaddingValues,
    uiState: WorkdayUiState.Success,
    projectName: String?,
    actions: WorkdayScreenActions
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
        HeaderSection(date = uiState.date, onClearDay = actions.onClearDay)

        projectName?.let {
            ProjectNameField(name = it)
        }

        if (uiState.isNewDay) {
            NewDayFields(uiState = uiState, actions = actions.fieldActions)
        } else {
            ExistingDayFields(uiState = uiState, actions = actions.fieldActions)
        }

        FooterSection(onConfirm = actions.onConfirm)
    }
}

@Composable
internal fun WorkdayStateContent(
    padding: PaddingValues,
    uiState: WorkdayUiState,
    projectName: String?,
    actions: WorkdayScreenActions
) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = uiState is WorkdayUiState.Loading
    )
    var lastSuccessState by remember { mutableStateOf<WorkdayUiState.Success?>(value = null) }

    LaunchedEffect(uiState) {
        if (uiState is WorkdayUiState.Success) {
            lastSuccessState = uiState
        }
    }

    when (uiState) {
        is WorkdayUiState.Loading -> {
            if (showLoadingIndicator) {
                WorkdayLoadingState(
                    padding = padding,
                    showLoadingIndicator = showLoadingIndicator
                )
            } else {
                lastSuccessState?.let { cachedState ->
                    WorkdayContent(
                        padding = padding,
                        uiState = cachedState,
                        projectName = projectName,
                        actions = actions
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
        is WorkdayUiState.Success -> WorkdayContent(
            padding = padding,
            uiState = uiState,
            projectName = projectName,
            actions = actions
        )
        is WorkdayUiState.Error -> WorkdayErrorState(
            padding = padding,
            errorMessage = uiState.message
        )
    }
}

internal data class WorkdayScreenActions(
    val onClearDay: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val fieldActions: WorkdayFieldActions = WorkdayFieldActions()
)

private fun createWorkdayScreenActions(
    viewModel: WorkdayViewModel,
    onConfirm: () -> Unit
): WorkdayScreenActions {
    return WorkdayScreenActions(
        onClearDay = viewModel::clearDay,
        onConfirm = onConfirm,
        fieldActions = WorkdayFieldActions(
            onCurrentStartTime = viewModel::currentStartTime,
            onSetStartTime = viewModel::setStartTime,
            onCurrentDailyWorkTime = viewModel::currentDailyWorkTime,
            onSetDailyWorkTime = viewModel::setDailyWorkTime,
            onCurrentLunchTime = viewModel::currentLunchTime,
            onSetLunchTime = viewModel::setLunchTime,
            onSetBalanceTotal = viewModel::setBalanceTotal,
            onSetWorkTimeTotal = viewModel::setWorkTimeTotal,
            onCurrentEndTime = viewModel::currentEndTime,
            onSetEndTime = viewModel::setEndTime,
            onCurrentWorkTimeToday = viewModel::currentWorkTimeToday,
            onSetWorkTimeToday = viewModel::setWorkTimeToday,
            onCurrentLunchStart = viewModel::currentLunchStart,
            onSetLunchStart = viewModel::setLunchStart,
            onCurrentLunchEnd = viewModel::currentLunchEnd,
            onSetLunchEnd = viewModel::setLunchEnd,
            onCurrentBreakStart = viewModel::currentBreakStart,
            onSetBreakStart = viewModel::setBreakStart,
            onCurrentBreakEnd = viewModel::currentBreakEnd,
            onSetBreakEnd = viewModel::setBreakEnd,
            onSetBalanceToday = viewModel::setBalanceToday
        )
    )
}

data class WorkdayArgs(
    val projectName: String? = null,
    val workday: WorkdayEntity? = null,
    val workStats: WorkStatsEntity? = null
)

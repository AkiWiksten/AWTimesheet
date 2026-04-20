package com.akiwiksten.worktime30.feature.projects.single.details

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.akiwiksten.worktime30.feature.projects.single.details.components.ExistingDayFields
import com.akiwiksten.worktime30.feature.projects.single.details.components.FooterSection
import com.akiwiksten.worktime30.feature.projects.single.details.components.HeaderSection
import com.akiwiksten.worktime30.feature.projects.single.details.components.NewDayFields
import com.akiwiksten.worktime30.feature.projects.single.details.components.ProjectDetailsFieldActions
import com.akiwiksten.worktime30.feature.projects.single.details.components.ProjectNameField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    args: ProjectDetailsArgs,
    onNavigateBack: () -> Unit,
    onConfirm: (ProjectDetailsState, WorkStatsState) -> Unit,
    viewModel: ProjectDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(onBack = onNavigateBack)

    LaunchedEffect(Unit) {
        args.projectName?.let { viewModel.setProjectName(projectName = it) }
        viewModel.loadProjectDetails(
            projectDetailsArg = args.projectDetails,
            workStatsArg = args.workStats
        )
    }

    Scaffold(
        topBar = {
            ProjectDetailsTopBar(onNavigateBack = onNavigateBack)
        }
    ) { padding ->
        val actions = remember(viewModel, onConfirm, uiState) {
            createProjectDetailsScreenActions(viewModel = viewModel) {
                val successState = uiState as? ProjectDetailsUiState.Success ?: return@createProjectDetailsScreenActions
                onConfirm(successState.data, successState.data.workStats)
            }
        }

        ProjectDetailsStateContent(
            padding = padding,
            uiState = uiState,
            projectName = args.projectName,
            actions = actions
        )
    }
}

@Composable
internal fun ProjectDetailsLoadingState(padding: PaddingValues, showLoadingIndicator: Boolean) {
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
internal fun ProjectDetailsErrorState(padding: PaddingValues, errorMessage: String) {
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
internal fun ProjectDetailsTopBar(onNavigateBack: () -> Unit) {
    Surface(
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        CenterAlignedTopAppBar(
            title = {
                Header(
                    title = stringResource(id = R.string.project_details),
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
}

@Composable
internal fun ProjectDetailsContent(
    padding: PaddingValues,
    uiState: ProjectDetailsUiState.Success,
    projectName: String?,
    actions: ProjectDetailsScreenActions
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
        ProjectDetailsHeaderGroup(
            date = uiState.data.date,
            projectName = projectName,
            onClearDay = actions.onClearDay
        )

        if (uiState.data.isNewDay) {
            NewDayFields(uiState = uiState, actions = actions.fieldActions)
        } else {
            ExistingDayFields(uiState = uiState, actions = actions.fieldActions)
        }

        FooterSection(onConfirm = actions.onConfirm)
    }
}

@Composable
private fun ProjectDetailsHeaderGroup(
    date: String,
    projectName: String?,
    onClearDay: () -> Unit
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(space = 12.dp)
        ) {
            HeaderSection(date = date, onClearDay = onClearDay)
            projectName?.let { ProjectNameField(name = it) }

            Text(
                text = stringResource(id = R.string.new_day),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
internal fun ProjectDetailsStateContent(
    padding: PaddingValues,
    uiState: ProjectDetailsUiState,
    projectName: String?,
    actions: ProjectDetailsScreenActions
) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = uiState is ProjectDetailsUiState.Loading
    )
    var lastSuccessState by remember { mutableStateOf<ProjectDetailsUiState.Success?>(value = null) }

    LaunchedEffect(uiState) {
        if (uiState is ProjectDetailsUiState.Success) {
            lastSuccessState = uiState
        }
    }

    when (uiState) {
        is ProjectDetailsUiState.Loading -> {
            if (showLoadingIndicator) {
                ProjectDetailsLoadingState(
                    padding = padding,
                    showLoadingIndicator = showLoadingIndicator
                )
            } else {
                lastSuccessState?.let { cachedState ->
                    ProjectDetailsContent(
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
        is ProjectDetailsUiState.Success -> ProjectDetailsContent(
            padding = padding,
            uiState = uiState,
            projectName = projectName,
            actions = actions
        )
        is ProjectDetailsUiState.Error -> ProjectDetailsErrorState(
            padding = padding,
            errorMessage = uiState.message
        )
    }
}

internal data class ProjectDetailsScreenActions(
    val onClearDay: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val fieldActions: ProjectDetailsFieldActions = ProjectDetailsFieldActions()
)

private fun createProjectDetailsScreenActions(
    viewModel: ProjectDetailsViewModel,
    onConfirm: () -> Unit
): ProjectDetailsScreenActions {
    return ProjectDetailsScreenActions(
        onClearDay = viewModel::clearDay,
        onConfirm = onConfirm,
        fieldActions = ProjectDetailsFieldActions(
            onCurrentStartTime = viewModel::currentStartTime,
            onSetStartTime = viewModel::setStartTime,
            onCurrentLunchTime = viewModel::currentLunchTime,
            onSetLunchTime = viewModel::setLunchTime,
            onCurrentEndTime = viewModel::currentEndTime,
            onSetEndTime = viewModel::setEndTime,
            onCurrentProjectTime = viewModel::currentProjectTime,
            onSetProjectTime = viewModel::setProjectTime,
            onCurrentLunchStart = viewModel::currentLunchStart,
            onSetLunchStart = viewModel::setLunchStart,
            onCurrentLunchEnd = viewModel::currentLunchEnd,
            onSetLunchEnd = viewModel::setLunchEnd,
            onCurrentBreakStart = viewModel::currentBreakStart,
            onSetBreakStart = viewModel::setBreakStart,
            onCurrentBreakEnd = viewModel::currentBreakEnd,
            onSetBreakEnd = viewModel::setBreakEnd
        )
    )
}

data class ProjectDetailsArgs(
    val projectName: String? = null,
    val projectDetails: ProjectDetailsState? = null,
    val workStats: WorkStatsState? = null
)

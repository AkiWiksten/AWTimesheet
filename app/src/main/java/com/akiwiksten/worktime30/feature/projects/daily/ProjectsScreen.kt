package com.akiwiksten.worktime30.feature.projects.daily

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.akiwiksten.worktime30.feature.projects.daily.components.ProjectsErrorContent
import com.akiwiksten.worktime30.feature.projects.daily.components.ProjectsLoadingContent
import com.akiwiksten.worktime30.feature.projects.daily.components.ProjectsSuccessContent
import com.akiwiksten.worktime30.core.ui.rememberDelayedLoadingVisibility

@Suppress("kotlin:S1854", "UNUSED_VALUE")
@Composable
fun ProjectsScreen(
    onNavigateToSingleProject: (Int) -> Unit,
    projectsViewModel: ProjectsViewModel = hiltViewModel(
        viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    ),
) {
    val projectsUiState by projectsViewModel.uiState.collectAsState()

    // Use state object directly to avoid SonarQube "unused assignment" false positives with 'by' delegate
    val selectedItemIndexState = remember { mutableIntStateOf(value = -1) }

    ProjectsContent(
        projectsUiState = projectsUiState,
        selectedItemIndex = selectedItemIndexState.intValue,
        actions = ProjectsActions(
            onSelectedItemIndexChange = { selectedItemIndexState.intValue = it },
            onNavigateToSingleProject = onNavigateToSingleProject,
            onRetry = projectsViewModel::retryLoad,
            onSaveWorkStats = projectsViewModel::updateWorkStats,
            onDeleteProject = { project ->
                projectsViewModel.deleteProject(state = project)
                selectedItemIndexState.intValue = -1
            }
        )
    )
}

@Composable
internal fun ProjectsContent(
    projectsUiState: ProjectsUiState,
    selectedItemIndex: Int,
    actions: ProjectsActions
) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = projectsUiState is ProjectsUiState.Loading
    )
    var lastSuccessState by remember { mutableStateOf<ProjectsUiState.Success?>(value = null) }

    LaunchedEffect(projectsUiState) {
        if (projectsUiState is ProjectsUiState.Success) {
            lastSuccessState = projectsUiState
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp)
    ) {
        when (projectsUiState) {
            is ProjectsUiState.Loading -> ProjectsLoadingContent(
                showLoadingIndicator = showLoadingIndicator,
                cachedState = lastSuccessState,
                selectedItemIndex = selectedItemIndex,
                actions = actions
            )

            is ProjectsUiState.Success -> ProjectsSuccessContent(
                state = projectsUiState,
                selectedItemIndex = selectedItemIndex,
                actions = actions
            )

            is ProjectsUiState.Error -> ProjectsErrorContent(
                message = projectsUiState.message,
                onRetry = actions.onRetry
            )
        }
    }
}

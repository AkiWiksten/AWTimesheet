package com.akiwiksten.worktime30.feature.workday

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
import com.akiwiksten.worktime30.core.FORM_SECTION_SPACING
import com.akiwiksten.worktime30.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.worktime30.feature.workday.components.WorkdayErrorContent
import com.akiwiksten.worktime30.feature.workday.components.WorkdayLoadingContent
import com.akiwiksten.worktime30.feature.workday.components.WorkdaySuccessContent

@Suppress("kotlin:S1854", "UNUSED_VALUE")
@Composable
fun WorkdayScreen(
    onNavigateToSingleProject: (Int) -> Unit,
    workdayViewModel: WorkdayViewModel = hiltViewModel(
        viewModelStoreOwner = LocalActivity.current as ViewModelStoreOwner
    ),
) {
    val workdayUiState by workdayViewModel.uiState.collectAsState()

    // Use state object directly to avoid SonarQube "unused assignment" false positives with 'by' delegate
    val selectedItemIndexState = remember { mutableIntStateOf(value = -1) }

    WorkdayContent(
        workdayUiState = workdayUiState,
        selectedItemIndex = selectedItemIndexState.intValue,
        actions = WorkdayActions(
            onSelectedItemIndexChange = { selectedItemIndexState.intValue = it },
            onNavigateToSingleProject = onNavigateToSingleProject,
            onRetry = workdayViewModel::retryLoad,
            onSaveWorkStats = workdayViewModel::updateWorkStats,
            onDeleteProject = { project ->
                workdayViewModel.deleteProject(state = project)
                selectedItemIndexState.intValue = -1
            }
        )
    )
}

@Composable
internal fun WorkdayContent(
    workdayUiState: WorkdayUiState,
    selectedItemIndex: Int,
    actions: WorkdayActions
) {
    val showLoadingIndicator = rememberDelayedLoadingVisibility(
        isLoading = workdayUiState is WorkdayUiState.Loading
    )
    var lastSuccessState by remember { mutableStateOf<WorkdayUiState.Success?>(value = null) }

    LaunchedEffect(workdayUiState) {
        if (workdayUiState is WorkdayUiState.Success) {
            lastSuccessState = workdayUiState
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)
    ) {
        when (workdayUiState) {
            is WorkdayUiState.Loading -> WorkdayLoadingContent(
                showLoadingIndicator = showLoadingIndicator,
                cachedState = lastSuccessState,
                selectedItemIndex = selectedItemIndex,
                actions = actions
            )

            is WorkdayUiState.Success -> WorkdaySuccessContent(
                state = workdayUiState,
                selectedItemIndex = selectedItemIndex,
                actions = actions
            )

            is WorkdayUiState.Error -> WorkdayErrorContent(
                message = workdayUiState.message,
                onRetry = actions.onRetry
            )
        }
    }
}

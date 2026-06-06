package com.akiwiksten.awtimesheet.feature.workday

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akiwiksten.awtimesheet.core.FORM_SECTION_SPACING
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumn
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumnState
import com.akiwiksten.awtimesheet.core.ui.rememberDelayedLoadingVisibility
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.workday.components.WorkdayErrorContent
import com.akiwiksten.awtimesheet.feature.workday.components.WorkdayLoadingContent
import com.akiwiksten.awtimesheet.feature.workday.components.WorkdaySuccessContent
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayActions
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayUiState

@Suppress("kotlin:S1854", "UNUSED_VALUE")
@Composable
fun WorkdayScreen(
    onNavigateToSingleProject: (SingleProjectState) -> Unit,
    workdayViewModel: WorkdayViewModel = hiltViewModel(),
) {
    val flexDayWorkType = stringResource(id = com.akiwiksten.awtimesheet.core.R.string.work_type_flex_day)

    LaunchedEffect(flexDayWorkType) {
        workdayViewModel.setLocalizedFlexDayWorkType(flexDayWorkType)
    }

    val pendingOldFlexTimeByDateState = rememberSaveable { mutableStateOf<String?>(value = null) }
    val pendingOldWorkTimeByDateState = rememberSaveable { mutableStateOf<String?>(value = null) }

    LifecycleResumeEffect(Unit) {
        val oldFlexTimeByDate = pendingOldFlexTimeByDateState.value
        val oldWorkTimeByDate = pendingOldWorkTimeByDateState.value

        if (oldFlexTimeByDate != null && oldWorkTimeByDate != null) {
            workdayViewModel.reconcileFlexTimeTotalAfterProjectEditorReturn(
                oldFlexTimeByDate = oldFlexTimeByDate,
                oldWorkTimeByDate = oldWorkTimeByDate
            )
            pendingOldFlexTimeByDateState.value = null
            pendingOldWorkTimeByDateState.value = null
        }

        onPauseOrDispose { }
    }

    val workdayUiState by workdayViewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    // Use state object directly to avoid SonarQube "unused assignment" false positives with 'by' delegate
    val selectedItemIndexState = rememberSaveable { mutableIntStateOf(value = -1) }
    val actions = remember(workdayViewModel, onNavigateToSingleProject) {
        WorkdayActions(
            onSelectedItemIndexChange = { selectedItemIndexState.intValue = it },
            onTrackProjectEditorLaunch = { oldFlexTimeByDate, oldWorkTimeByDate ->
                pendingOldFlexTimeByDateState.value = oldFlexTimeByDate
                pendingOldWorkTimeByDateState.value = oldWorkTimeByDate
            },
            onNavigateToSingleProject = onNavigateToSingleProject,
            onRetry = workdayViewModel::retryLoad,
            onSaveSettings = workdayViewModel::updateSettings,
            onDeleteProject = { project ->
                workdayViewModel.deleteProject(state = project)
            }
        )
    }

    WorkdayContent(
        workdayUiState = workdayUiState,
        selectedItemIndex = selectedItemIndexState.intValue,
        scrollState = scrollState,
        actions = actions
    )
}

@Composable
internal fun WorkdayContent(
    workdayUiState: WorkdayUiState,
    selectedItemIndex: Int,
    scrollState: androidx.compose.foundation.ScrollState,
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

    ScrollableScreenColumn(
        state = ScrollableScreenColumnState(
            scrollState = scrollState,
            modifier = Modifier.fillMaxSize(),
            columnModifier = Modifier
                .fillMaxSize()
                .padding(all = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)
        )
    ) {
        when (workdayUiState) {
            is WorkdayUiState.Loading -> WorkdayLoadingContent(
                showLoadingIndicator = showLoadingIndicator,
                cachedState = lastSuccessState,
                selectedItemIndex = selectedItemIndex,
                actions = actions
            )

            is WorkdayUiState.Success -> Column(
                verticalArrangement = Arrangement.spacedBy(FORM_SECTION_SPACING)
            ) {
                WorkdaySuccessContent(
                    state = workdayUiState,
                    selectedItemIndex = selectedItemIndex,
                    actions = actions
                )
            }

            is WorkdayUiState.Error -> WorkdayErrorContent(
                message = workdayUiState.message,
                onRetry = actions.onRetry
            )
        }
    }
}

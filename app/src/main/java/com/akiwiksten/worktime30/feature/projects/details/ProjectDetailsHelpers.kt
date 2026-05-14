package com.akiwiksten.worktime30.feature.projects.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState

@Composable
internal fun rememberBaselineData(
    uiState: ProjectDetailsUiState,
    isInitialLoadComplete: Boolean,
    projectDetails: ProjectDetailsState,
): ProjectDetailsState? {
    var initialData by remember { mutableStateOf<ProjectDetailsState?>(value = null) }
    var isBaselineInitialized by remember { mutableStateOf(value = false) }

    LaunchedEffect(uiState, isInitialLoadComplete) {
        val successState = uiState as? ProjectDetailsUiState.Success ?: return@LaunchedEffect
        if (isBaselineInitialized || !isInitialLoadComplete) return@LaunchedEffect
        val data = successState.details
        if (data.date.isNotBlank() && data.matchesInitialProjectDetails(projectDetails = projectDetails)) {
            initialData = data
            isBaselineInitialized = true
        }
    }

    return initialData
}

internal fun ProjectDetailsState.matchesInitialProjectDetails(projectDetails: ProjectDetailsState): Boolean {
    val expectedProjectTime = ProjectDetailsUiMapper.normalizeProjectTimeOnOpen(
        startTime = projectDetails.startTime.ifEmpty { ZERO_TIME },
        endTime = projectDetails.endTime.ifEmpty { ZERO_TIME },
        projectTime = projectDetails.projectTime.ifEmpty { ZERO_TIME }
    )
    val matchesDetails = startTime == projectDetails.startTime &&
        endTime == projectDetails.endTime &&
        lunchStart == projectDetails.lunchStart &&
        lunchEnd == projectDetails.lunchEnd &&
        breakStart == projectDetails.breakStart &&
        breakEnd == projectDetails.breakEnd &&
        projectTime == expectedProjectTime
    return matchesDetails
}

fun createProjectDetailsScreenActions(
    viewModel: ProjectDetailsViewModel,
    onConfirm: () -> Unit
): ProjectDetailsScreenActions {
    return ProjectDetailsScreenActions(
        onClearDetails = viewModel.clearDetails,
        onConfirm = onConfirm,
        fieldActions = ProjectDetailsFieldActions(
            startTime = TimeFieldAction(
                onCurrent = viewModel.currentStartTime,
                onSet = viewModel.setStartTime
            ),
            lunchTime = TimeFieldAction(
                onCurrent = viewModel.currentLunchTime,
                onSet = viewModel.setLunchTime
            ),
            endTime = TimeFieldAction(
                onCurrent = viewModel.currentEndTime,
                onSet = viewModel.setEndTime
            ),
            projectTime = TimeFieldAction(
                onCurrent = viewModel.currentProjectTime,
                onSet = viewModel.setProjectTime
            ),
            lunchStart = TimeFieldAction(
                onCurrent = viewModel.currentLunchStart,
                onSet = viewModel.setLunchStart
            ),
            lunchEnd = TimeFieldAction(
                onCurrent = viewModel.currentLunchEnd,
                onSet = viewModel.setLunchEnd
            ),
            breakStart = TimeFieldAction(
                onCurrent = viewModel.currentBreakStart,
                onSet = viewModel.setBreakStart
            ),
            breakEnd = TimeFieldAction(
                onCurrent = viewModel.currentBreakEnd,
                onSet = viewModel.setBreakEnd
            )
        )
    )
}

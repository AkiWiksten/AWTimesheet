package com.akiwiksten.awtimesheet.feature.projectdetails.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.feature.projectdetails.ProjectDetailsUiState
import com.akiwiksten.awtimesheet.feature.projectdetails.ProjectDetailsViewModel

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
            startTime = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.START_TIME) },
                onSet = { viewModel.updateTime(ProjectDetailsField.START_TIME, it) }
            ),
            lunchTime = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.LUNCH_TIME) },
                onSet = { viewModel.updateTime(ProjectDetailsField.LUNCH_TIME, it) }
            ),
            endTime = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.END_TIME) },
                onSet = { viewModel.updateTime(ProjectDetailsField.END_TIME, it) }
            ),
            projectTime = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.PROJECT_TIME) },
                onSet = { viewModel.updateTime(ProjectDetailsField.PROJECT_TIME, it) }
            ),
            lunchStart = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.LUNCH_START) },
                onSet = { viewModel.updateTime(ProjectDetailsField.LUNCH_START, it) }
            ),
            lunchEnd = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.LUNCH_END) },
                onSet = { viewModel.updateTime(ProjectDetailsField.LUNCH_END, it) }
            ),
            breakStart = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.BREAK_START) },
                onSet = { viewModel.updateTime(ProjectDetailsField.BREAK_START, it) }
            ),
            breakEnd = ProjectDetailsTimeFieldAction(
                onCurrent = { viewModel.currentTime(ProjectDetailsField.BREAK_END) },
                onSet = { viewModel.updateTime(ProjectDetailsField.BREAK_END, it) }
            )
        )
    )
}

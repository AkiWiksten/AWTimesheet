package com.akiwiksten.worktime30.feature.projects.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.ui.UnsavedChangesDialog
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState

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

@Composable
internal fun UnsavedChangesSection(
    showState: MutableState<Boolean>,
    uiState: ProjectDetailsUiState,
    unsavedMessage: String,
    onNavigateBack: () -> Unit,
    onConfirm: (ProjectDetailsState, SettingsState) -> Unit,
) {
    if (!showState.value) return
    val successState = uiState as? ProjectDetailsUiState.Success
    UnsavedChangesDialog(
        onDismiss = { showState.value = false },
        onDiscard = onNavigateBack,
        onSave = successState?.let {
            {
                onConfirm(
                    it.details,
                    it.settings.copy(dailyLunchTimeEstimate = it.details.lunchTimeEstimate)
                )
            }
        },
        dialogText = unsavedMessage
    )
}

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

@Composable
internal fun rememberBaselineData(
    uiState: ProjectDetailsUiState,
    isInitialLoadComplete: Boolean,
    args: ProjectDetailsArgs,
): ProjectDetailsState? {
    var initialData by remember { mutableStateOf<ProjectDetailsState?>(value = null) }
    var isBaselineInitialized by remember { mutableStateOf(value = false) }

    LaunchedEffect(uiState, isInitialLoadComplete) {
        val successState = uiState as? ProjectDetailsUiState.Success ?: return@LaunchedEffect
        if (isBaselineInitialized || !isInitialLoadComplete) return@LaunchedEffect
        val data = successState.data
        if (data.date.isNotBlank() && data.matchesArgs(args)) {
            initialData = data
            isBaselineInitialized = true
        }
    }

    return initialData
}

internal fun ProjectDetailsState.matchesArgs(args: ProjectDetailsArgs): Boolean {
    val projectDetailsArg = args.projectDetails ?: return args.workStats == null || workStats == args.workStats
    val expectedProjectTime = ProjectDetailsUiMapper.normalizeProjectTimeOnOpen(
        startTime = projectDetailsArg.startTime.ifEmpty { ZERO_TIME },
        endTime = projectDetailsArg.endTime.ifEmpty { ZERO_TIME },
        projectTime = projectDetailsArg.projectTime.ifEmpty { ZERO_TIME }
    )
    val matchesDetails = startTime == projectDetailsArg.startTime &&
        endTime == projectDetailsArg.endTime &&
        lunchStart == projectDetailsArg.lunchStart &&
        lunchEnd == projectDetailsArg.lunchEnd &&
        breakStart == projectDetailsArg.breakStart &&
        breakEnd == projectDetailsArg.breakEnd &&
        projectTime == expectedProjectTime
    return matchesDetails && (args.workStats == null || workStats == args.workStats)
}

@Composable
internal fun UnsavedChangesSection(
    showState: MutableState<Boolean>,
    uiState: ProjectDetailsUiState,
    unsavedMessage: String,
    onNavigateBack: () -> Unit,
    onConfirm: (ProjectDetailsState, WorkStatsState) -> Unit,
) {
    if (!showState.value) return
    val successState = uiState as? ProjectDetailsUiState.Success
    UnsavedChangesDialog(
        onDismiss = { showState.value = false },
        onDiscard = onNavigateBack,
        onSave = successState?.let { { onConfirm(it.data, it.data.workStats) } },
        dialogText = unsavedMessage
    )
}

package com.akiwiksten.worktime30.feature.projects.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.ui.verticalScrollbar
import com.akiwiksten.worktime30.domain.model.isNewDayForProject
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsScreenActions
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsUiState

@Composable
internal fun ProjectDetailsLoadingState(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(all = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
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

@Composable
internal fun ProjectDetailsSuccessState(
    padding: PaddingValues,
    uiState: ProjectDetailsUiState.Success,
    actions: ProjectDetailsScreenActions,
    isConfirmEnabled: Boolean
) {
    val scrollState = rememberScrollState()
    val helperTextResId = when {
        uiState.details.isNewDayForProject() -> R.string.add_new_project_details
        uiState.details.startTime != ZERO_TIME && uiState.details.projectTime == ZERO_TIME -> R.string.select_end_time
        else -> R.string.done_project
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = padding)
            .verticalScrollbar(scrollState = scrollState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp, 16.dp, 16.dp, 0.dp)
                .verticalScroll(state = scrollState),
            verticalArrangement = Arrangement.spacedBy(space = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderSection(
                date = uiState.details.date,
                projectName = uiState.details.projectName,
                helperTextResId = helperTextResId,
                onClearDetails = actions.onClearDetails
            )

            if (uiState.details.isNewDayForProject()) {
                NewDayForProjectSection(uiState = uiState, actions = actions.fieldActions)
            } else {
                ExistingDayForProjectSection(uiState = uiState, actions = actions.fieldActions)
            }

            FooterSection(onConfirm = actions.onConfirm, isConfirmEnabled = isConfirmEnabled)
        }
    }
}

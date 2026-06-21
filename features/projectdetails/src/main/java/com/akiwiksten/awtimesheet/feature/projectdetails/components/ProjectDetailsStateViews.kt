package com.akiwiksten.awtimesheet.feature.projectdetails.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.ui.CenteredErrorBox
import com.akiwiksten.awtimesheet.core.ui.CenteredLoadingBox
import com.akiwiksten.awtimesheet.core.ui.LocalContentBottomPadding
import com.akiwiksten.awtimesheet.core.ui.NoteBanner
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumn
import com.akiwiksten.awtimesheet.core.ui.ScrollableScreenColumnState
import com.akiwiksten.awtimesheet.domain.model.isNewDayForProject
import com.akiwiksten.awtimesheet.feature.projectdetails.ProjectDetailsUiState
import com.akiwiksten.awtimesheet.feature.projectdetails.R
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsScreenActions
import com.akiwiksten.awtimesheet.core.R as CoreR

@Composable
internal fun ProjectDetailsLoadingState(padding: PaddingValues) {
    CenteredLoadingBox(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    )
}

@Composable
internal fun ProjectDetailsErrorState(padding: PaddingValues, errorMessage: String) {
    CenteredErrorBox(
        errorMessage = errorMessage,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    )
}

@Composable
internal fun ProjectDetailsSuccessState(
    padding: PaddingValues,
    uiState: ProjectDetailsUiState.Success,
    actions: ProjectDetailsScreenActions,
    isConfirmEnabled: Boolean,
    isAddMode: Boolean = true
) {
    val scrollState = rememberScrollState()
    val helperTextResId = when {
        uiState.details.isNewDayForProject() -> R.string.add_new_project_details
        uiState.details.startTime != ZERO_TIME && uiState.details.projectTime == ZERO_TIME -> R.string.select_end_time
        else -> R.string.done_project
    }

    ScrollableScreenColumn(
        state = ScrollableScreenColumnState(
            scrollState = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding),
            columnModifier = Modifier
                .fillMaxSize()
                .padding(PADDING_SPACING, PADDING_SPACING, PADDING_SPACING, 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING)
        )
    ) {
        ProjectDetailsHeaderSection(
            date = uiState.details.date,
            projectName = uiState.details.projectName,
            onClearDetails = actions.onClearDetails
        )

        NoteBanner(text = stringResource(id = helperTextResId))

        if (!isAddMode && isConfirmEnabled) {
            NoteBanner(text = stringResource(id = CoreR.string.edit_mode_modified_note))
        }

        if (uiState.details.isNewDayForProject()) {
            ProjectDetailsNewDayForProjectSection(uiState = uiState, actions = actions.fieldActions)
        } else {
            ProjectDetailsExistingDayForProjectSection(uiState = uiState, actions = actions.fieldActions)
        }

        ProjectDetailsFooterSection(onConfirm = actions.onConfirm, isConfirmEnabled = isConfirmEnabled)
        Spacer(modifier = Modifier.padding(bottom = LocalContentBottomPadding.current))
    }
}

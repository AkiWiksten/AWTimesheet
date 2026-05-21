package com.akiwiksten.awtimesheet.feature.projects.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.FORM_GROUP_PADDING
import com.akiwiksten.awtimesheet.core.FORM_GROUP_SPACING
import com.akiwiksten.awtimesheet.core.FORM_SECTION_SPACING
import com.akiwiksten.awtimesheet.core.HEADER_CONTENT_PADDING
import com.akiwiksten.awtimesheet.core.HEADER_CONTENT_SPACING
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.feature.projects.details.ProjectDetailsFieldActions
import com.akiwiksten.awtimesheet.feature.projects.details.ProjectDetailsTimeRowLabels
import com.akiwiksten.awtimesheet.feature.projects.details.ProjectDetailsUiState

@Composable
internal fun ProjectDetailsHeaderSection(
    date: String,
    onClearDetails: () -> Unit,
    projectName: String?,
    helperTextResId: Int?
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HEADER_CONTENT_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = HEADER_CONTENT_SPACING)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onClearDetails,
                shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(text = stringResource(id = R.string.clear_details))
            }
            ProjectDetailsNameField(name = projectName.orEmpty())

            helperTextResId?.let { textResId ->
                Text(
                    text = stringResource(id = textResId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
internal fun ProjectDetailsNewDayForProjectSection(
    uiState: ProjectDetailsUiState.Success,
    actions: ProjectDetailsFieldActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)) {
        ElevatedCard(
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(all = FORM_GROUP_PADDING),
                verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)
            ) {
                ProjectDetailsTimeRow(
                    textFieldValue = uiState.details.startTime,
                    stringId = R.string.start_time,
                    currentTime = actions.startTime.onCurrent,
                    onConfirmation = actions.startTime.onSet,
                    labels = ProjectDetailsTimeRowLabels(
                        currentTimeLabelId = R.string.now,
                        timePickerLabelId = R.string.pick
                    )
                )
                ProjectDetailsTimeRow(
                    textFieldValue = uiState.details.lunchTimeEstimate,
                    stringId = R.string.lunch_time,
                    currentTime = actions.lunchTime.onCurrent,
                    onConfirmation = actions.lunchTime.onSet
                )
                ProjectDetailsTimeRow(
                    textFieldValue = uiState.details.projectTime,
                    stringId = R.string.project_time,
                    currentTime = actions.projectTime.onCurrent,
                    onConfirmation = actions.projectTime.onSet
                )
            }
        }
    }
}

@Composable
internal fun ProjectDetailsExistingDayForProjectSection(
    uiState: ProjectDetailsUiState.Success,
    actions: ProjectDetailsFieldActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)) {
        MainWorkTimeSection(uiState = uiState, actions = actions)

        LunchAndBreakSection(uiState = uiState, actions = actions)

        DailySummarySection(uiState = uiState, actions = actions)
    }
}

@Composable
private fun MainWorkTimeSection(uiState: ProjectDetailsUiState.Success, actions: ProjectDetailsFieldActions) {
    val endTimeLabelId = if (uiState.details.projectTime == ZERO_TIME) {
        R.string.estimated_end_time
    } else {
        R.string.end_time
    }

    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(all = FORM_GROUP_PADDING),
            verticalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)
        ) {
            ProjectDetailsTimeRow(
                textFieldValue = uiState.details.startTime,
                stringId = R.string.start_time,
                currentTime = actions.startTime.onCurrent,
                onConfirmation = actions.startTime.onSet,
                labels = ProjectDetailsTimeRowLabels(
                    currentTimeLabelId = R.string.now,
                    timePickerLabelId = R.string.pick
                )
            )
            ProjectDetailsTimeRow(
                textFieldValue = uiState.details.endTime,
                stringId = endTimeLabelId,
                currentTime = actions.endTime.onCurrent,
                onConfirmation = actions.endTime.onSet
            )
            ProjectDetailsTimeRow(
                textFieldValue = uiState.details.projectTime,
                stringId = R.string.project_time,
                currentTime = actions.projectTime.onCurrent,
                onConfirmation = actions.projectTime.onSet
            )
        }
    }
}

@Composable
private fun LunchAndBreakSection(uiState: ProjectDetailsUiState.Success, actions: ProjectDetailsFieldActions) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(all = FORM_GROUP_PADDING),
            verticalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)
        ) {
            ProjectDetailsTimeRow(
                textFieldValue = uiState.details.lunchStart,
                stringId = R.string.lunch_start,
                currentTime = actions.lunchStart.onCurrent,
                onConfirmation = actions.lunchStart.onSet
            )
            ProjectDetailsTimeRow(
                textFieldValue = uiState.details.lunchEnd,
                stringId = R.string.lunch_end,
                currentTime = actions.lunchEnd.onCurrent,
                onConfirmation = actions.lunchEnd.onSet
            )
            ProjectDetailsTimeRow(
                textFieldValue = uiState.details.breakStart,
                stringId = R.string.break_start,
                currentTime = actions.breakStart.onCurrent,
                onConfirmation = actions.breakStart.onSet
            )
            ProjectDetailsTimeRow(
                textFieldValue = uiState.details.breakEnd,
                stringId = R.string.break_end,
                currentTime = actions.breakEnd.onCurrent,
                onConfirmation = actions.breakEnd.onSet
            )
        }
    }
}

@Composable
private fun DailySummarySection(uiState: ProjectDetailsUiState.Success, actions: ProjectDetailsFieldActions) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(all = FORM_GROUP_PADDING),
            verticalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)
        ) {
            ProjectDetailsTimeRow(
                textFieldValue = uiState.details.lunchTimeEstimate,
                stringId = R.string.lunch_time,
                currentTime = actions.lunchTime.onCurrent,
                onConfirmation = actions.lunchTime.onSet
            )
        }
    }
}

@Composable
internal fun ProjectDetailsFooterSection(onConfirm: () -> Unit, isConfirmEnabled: Boolean) {
    Button(
        onClick = onConfirm,
        enabled = isConfirmEnabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Text(text = stringResource(id = R.string.confirm), style = MaterialTheme.typography.titleLarge)
    }
}

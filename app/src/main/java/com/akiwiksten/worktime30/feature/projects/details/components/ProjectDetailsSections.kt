package com.akiwiksten.worktime30.feature.projects.details.components

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
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.FIELD_CORNER_RADIUS
import com.akiwiksten.worktime30.core.FORM_GROUP_PADDING
import com.akiwiksten.worktime30.core.FORM_GROUP_SPACING
import com.akiwiksten.worktime30.core.FORM_SECTION_SPACING
import com.akiwiksten.worktime30.core.HEADER_CONTENT_PADDING
import com.akiwiksten.worktime30.core.HEADER_CONTENT_SPACING
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsFieldActions
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsUiState
import com.akiwiksten.worktime30.feature.projects.details.TimeRowLabels

@Composable
fun HeaderSection(
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
            ProjectNameField(name = projectName.orEmpty())

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
fun NewDayForProjectSection(uiState: ProjectDetailsUiState.Success, actions: ProjectDetailsFieldActions) {
    Column(verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)) {
        ElevatedCard(
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(all = FORM_GROUP_PADDING),
                verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)
            ) {
                AddTimeRow(
                    textFieldValue = uiState.details.startTime,
                    stringId = R.string.start_time,
                    currentTime = actions.startTime.onCurrent,
                    onConfirmation = actions.startTime.onSet,
                    labels = TimeRowLabels(
                        currentTimeLabelId = R.string.now,
                        timePickerLabelId = R.string.pick
                    )
                )
                AddTimeRow(
                    textFieldValue = uiState.details.lunchTimeEstimate,
                    stringId = R.string.lunch_time,
                    currentTime = actions.lunchTime.onCurrent,
                    onConfirmation = actions.lunchTime.onSet
                )
                AddTimeRow(
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
fun ExistingDayForProjectSection(uiState: ProjectDetailsUiState.Success, actions: ProjectDetailsFieldActions) {
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
            AddTimeRow(
                textFieldValue = uiState.details.startTime,
                stringId = R.string.start_time,
                currentTime = actions.startTime.onCurrent,
                onConfirmation = actions.startTime.onSet,
                labels = TimeRowLabels(currentTimeLabelId = R.string.now, timePickerLabelId = R.string.pick)
            )
            AddTimeRow(
                textFieldValue = uiState.details.endTime,
                stringId = endTimeLabelId,
                currentTime = actions.endTime.onCurrent,
                onConfirmation = actions.endTime.onSet
            )
            AddTimeRow(
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
            AddTimeRow(
                textFieldValue = uiState.details.lunchStart,
                stringId = R.string.lunch_start,
                currentTime = actions.lunchStart.onCurrent,
                onConfirmation = actions.lunchStart.onSet
            )
            AddTimeRow(
                textFieldValue = uiState.details.lunchEnd,
                stringId = R.string.lunch_end,
                currentTime = actions.lunchEnd.onCurrent,
                onConfirmation = actions.lunchEnd.onSet
            )
            AddTimeRow(
                textFieldValue = uiState.details.breakStart,
                stringId = R.string.break_start,
                currentTime = actions.breakStart.onCurrent,
                onConfirmation = actions.breakStart.onSet
            )
            AddTimeRow(
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
            AddTimeRow(
                textFieldValue = uiState.details.lunchTimeEstimate,
                stringId = R.string.lunch_time,
                currentTime = actions.lunchTime.onCurrent,
                onConfirmation = actions.lunchTime.onSet
            )
        }
    }
}

@Composable
fun FooterSection(onConfirm: () -> Unit, isConfirmEnabled: Boolean) {
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

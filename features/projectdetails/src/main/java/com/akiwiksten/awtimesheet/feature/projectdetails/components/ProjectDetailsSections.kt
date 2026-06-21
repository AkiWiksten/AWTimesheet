package com.akiwiksten.awtimesheet.feature.projectdetails.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.PADDING_SPACING
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.ui.AwtButton
import com.akiwiksten.awtimesheet.feature.projectdetails.ProjectDetailsUiState
import com.akiwiksten.awtimesheet.feature.projectdetails.R
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsFieldActions
import com.akiwiksten.awtimesheet.feature.projectdetails.model.ProjectDetailsTimeRowLabels

@Composable
internal fun ProjectDetailsHeaderSection(
    date: String,
    onClearDetails: () -> Unit,
    projectName: String?,
) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PADDING_SPACING_SMALL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            AwtButton(
                onClick = onClearDetails,
            ) {
                Text(text = stringResource(id = R.string.clear_details))
            }
            ProjectDetailsNameField(name = projectName.orEmpty())
        }
    }
}

@Composable
internal fun ProjectDetailsNewDayForProjectSection(
    uiState: ProjectDetailsUiState.Success,
    actions: ProjectDetailsFieldActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING)) {
        ElevatedCard(
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(all = PADDING_SPACING_SMALL),
                verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING)
            ) {
                key("startTime") {
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
                }
                key("lunchTimeEstimate") {
                    ProjectDetailsTimeRow(
                        textFieldValue = uiState.details.lunchTimeEstimate,
                        stringId = R.string.lunch_time,
                        currentTime = actions.lunchTime.onCurrent,
                        onConfirmation = actions.lunchTime.onSet
                    )
                }
                key("projectTime") {
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
}

@Composable
internal fun ProjectDetailsExistingDayForProjectSection(
    uiState: ProjectDetailsUiState.Success,
    actions: ProjectDetailsFieldActions
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)) {
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
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(all = PADDING_SPACING_SMALL),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
        ) {
            key("startTime") {
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
            }
            key("endTime") {
                ProjectDetailsTimeRow(
                    textFieldValue = uiState.details.endTime,
                    stringId = endTimeLabelId,
                    currentTime = actions.endTime.onCurrent,
                    onConfirmation = actions.endTime.onSet
                )
            }
            key("projectTime") {
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
private fun LunchAndBreakSection(uiState: ProjectDetailsUiState.Success, actions: ProjectDetailsFieldActions) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(all = PADDING_SPACING_SMALL),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
        ) {
            key("lunchStart") {
                ProjectDetailsTimeRow(
                    textFieldValue = uiState.details.lunchStart,
                    stringId = R.string.lunch_start,
                    currentTime = actions.lunchStart.onCurrent,
                    onConfirmation = actions.lunchStart.onSet
                )
            }
            key("lunchEnd") {
                ProjectDetailsTimeRow(
                    textFieldValue = uiState.details.lunchEnd,
                    stringId = R.string.lunch_end,
                    currentTime = actions.lunchEnd.onCurrent,
                    onConfirmation = actions.lunchEnd.onSet
                )
            }
            key("breakStart") {
                ProjectDetailsTimeRow(
                    textFieldValue = uiState.details.breakStart,
                    stringId = R.string.break_start,
                    currentTime = actions.breakStart.onCurrent,
                    onConfirmation = actions.breakStart.onSet
                )
            }
            key("breakEnd") {
                ProjectDetailsTimeRow(
                    textFieldValue = uiState.details.breakEnd,
                    stringId = R.string.break_end,
                    currentTime = actions.breakEnd.onCurrent,
                    onConfirmation = actions.breakEnd.onSet
                )
            }
        }
    }
}

@Composable
private fun DailySummarySection(uiState: ProjectDetailsUiState.Success, actions: ProjectDetailsFieldActions) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(all = PADDING_SPACING_SMALL),
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
        ) {
            key("lunchTimeEstimate") {
                ProjectDetailsTimeRow(
                    textFieldValue = uiState.details.lunchTimeEstimate,
                    stringId = R.string.lunch_time,
                    currentTime = actions.lunchTime.onCurrent,
                    onConfirmation = actions.lunchTime.onSet
                )
            }
        }
    }
}

@Composable
internal fun ProjectDetailsFooterSection(onConfirm: () -> Unit, isConfirmEnabled: Boolean) {
    AwtButton(
        onClick = onConfirm,
        enabled = isConfirmEnabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = stringResource(id = R.string.confirm), style = MaterialTheme.typography.titleLarge)
    }
}

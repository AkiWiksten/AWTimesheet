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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.akiwiksten.worktime30.core.LABEL_FONT_SIZE_SCALE
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsUiState


@Composable
fun ProjectNameField(name: String) {
    OutlinedTextField(
        value = name,
        onValueChange = {},
        label = {
            Text(
                text = stringResource(id = R.string.project_name),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = MaterialTheme.typography.bodyLarge.fontSize * LABEL_FONT_SIZE_SCALE,
                    fontWeight = FontWeight.Bold
                )
            )
        },
        modifier = Modifier.fillMaxWidth(),
        readOnly = true,
        enabled = false,
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        textStyle = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
        )
    )
}

@Composable
fun HeaderSection(date: String, onClearDay: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = HEADER_CONTENT_PADDING,
                top = HEADER_CONTENT_PADDING,
                end = HEADER_CONTENT_PADDING
            ),
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
            onClick = onClearDay,
            shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text(text = stringResource(id = R.string.clear_day))
        }
    }
}

@Composable
fun NewDayFields(uiState: ProjectDetailsUiState.Success, actions: ProjectDetailsFieldActions) {
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
                    textFieldValue = uiState.data.startTime,
                    stringId = R.string.start_time,
                    currentTime = actions.onCurrentStartTime,
                    onConfirmation = actions.onSetStartTime,
                    currentTimeLabelId = R.string.now,
                    timePickerLabelId = R.string.pick
                )
                AddTimeRow(
                    textFieldValue = uiState.data.workStats.lunchTime,
                    stringId = R.string.lunch_time,
                    currentTime = actions.onCurrentLunchTime,
                    onConfirmation = actions.onSetLunchTime
                )
                AddTimeRow(
                    textFieldValue = uiState.data.projectTime,
                    stringId = R.string.project_time,
                    currentTime = actions.onCurrentProjectTime,
                    onConfirmation = actions.onSetProjectTime
                )
            }
        }
    }
}

@Composable
fun ExistingDayFields(uiState: ProjectDetailsUiState.Success, actions: ProjectDetailsFieldActions) {
    Column(verticalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)) {
        MainWorkTimeFields(uiState = uiState, actions = actions)

        LunchAndBreakFields(uiState = uiState, actions = actions)

        DailySummaryFields(uiState = uiState, actions = actions)
    }
}

@Composable
private fun MainWorkTimeFields(uiState: ProjectDetailsUiState.Success, actions: ProjectDetailsFieldActions) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(all = FORM_GROUP_PADDING),
            verticalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)
        ) {
            AddTimeRow(
                textFieldValue = uiState.data.startTime,
                stringId = R.string.start_time,
                currentTime = actions.onCurrentStartTime,
                onConfirmation = actions.onSetStartTime,
                currentTimeLabelId = R.string.now,
                timePickerLabelId = R.string.pick
            )
            AddTimeRow(
                textFieldValue = uiState.data.endTime,
                stringId = R.string.end_time,
                currentTime = actions.onCurrentEndTime,
                onConfirmation = actions.onSetEndTime
            )
            AddTimeRow(
                textFieldValue = uiState.data.projectTime,
                stringId = R.string.project_time,
                currentTime = actions.onCurrentProjectTime,
                onConfirmation = actions.onSetProjectTime
            )
        }
    }
}

@Composable
private fun LunchAndBreakFields(uiState: ProjectDetailsUiState.Success, actions: ProjectDetailsFieldActions) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(all = FORM_GROUP_PADDING),
            verticalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)
        ) {
            AddTimeRow(
                textFieldValue = uiState.data.lunchStart,
                stringId = R.string.lunch_start,
                currentTime = actions.onCurrentLunchStart,
                onConfirmation = actions.onSetLunchStart
            )
            AddTimeRow(
                textFieldValue = uiState.data.lunchEnd,
                stringId = R.string.lunch_end,
                currentTime = actions.onCurrentLunchEnd,
                onConfirmation = actions.onSetLunchEnd
            )
            AddTimeRow(
                textFieldValue = uiState.data.breakStart,
                stringId = R.string.break_start,
                currentTime = actions.onCurrentBreakStart,
                onConfirmation = actions.onSetBreakStart
            )
            AddTimeRow(
                textFieldValue = uiState.data.breakEnd,
                stringId = R.string.break_end,
                currentTime = actions.onCurrentBreakEnd,
                onConfirmation = actions.onSetBreakEnd
            )
        }
    }
}

@Composable
private fun DailySummaryFields(uiState: ProjectDetailsUiState.Success, actions: ProjectDetailsFieldActions) {
    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(all = FORM_GROUP_PADDING),
            verticalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)
        ) {
            AddTimeRow(
                textFieldValue = uiState.data.workStats.lunchTime,
                stringId = R.string.lunch_time,
                currentTime = actions.onCurrentLunchTime,
                onConfirmation = actions.onSetLunchTime
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

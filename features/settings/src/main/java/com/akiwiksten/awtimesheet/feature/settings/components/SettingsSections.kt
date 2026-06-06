package com.akiwiksten.awtimesheet.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.ACTION_BUTTON_FONT_SIZE
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.FORM_GROUP_SPACING
import com.akiwiksten.awtimesheet.core.FORM_INLINE_SPACING
import com.akiwiksten.awtimesheet.core.FORM_MAX_WIDTH
import com.akiwiksten.awtimesheet.core.HEADER_CONTENT_PADDING
import com.akiwiksten.awtimesheet.core.HEADER_CONTENT_SPACING
import com.akiwiksten.awtimesheet.core.ui.DropdownMenuBox
import com.akiwiksten.awtimesheet.core.ui.DropdownMenuField
import com.akiwiksten.awtimesheet.core.ui.Header
import com.akiwiksten.awtimesheet.core.ui.NoteBanner
import com.akiwiksten.awtimesheet.feature.settings.R
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsActionButtonsSectionState
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsWorkTypeSectionState

@Composable
internal fun SettingsActionButtonsSection(
    state: SettingsActionButtonsSectionState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = FORM_MAX_WIDTH),
        verticalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)
    ) {
        Button(
            onClick = state.onSave,
            enabled = state.isSaveEnabled,
            modifier = Modifier.fillMaxWidth(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.save), fontSize = ACTION_BUTTON_FONT_SIZE)
        }
        Button(
            onClick = state.onGenerateXlsx,
            enabled = state.isReportEnabled,
            modifier = Modifier.fillMaxWidth(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.generate_xlsx), fontSize = ACTION_BUTTON_FONT_SIZE)
        }
        Button(
            onClick = state.onGenerateWorkdaysForMonth,
            enabled = state.isReportEnabled,
            modifier = Modifier.fillMaxWidth(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.generate_workdays_month), fontSize = ACTION_BUTTON_FONT_SIZE)
        }
        Button(
            onClick = state.onGenerateWorkdaysForYear,
            enabled = state.isReportEnabled,
            modifier = Modifier.fillMaxWidth(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
        ) {
            Text(text = stringResource(id = R.string.generate_workdays_year), fontSize = ACTION_BUTTON_FONT_SIZE)
        }
        if (state.isReportEnabled) {
            NoteBanner(text = stringResource(id = R.string.monthly_help_xlsx))
        }
    }
}

@Composable
internal fun SettingsHeaderSection(date: String) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = FORM_MAX_WIDTH),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(all = HEADER_CONTENT_PADDING),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = HEADER_CONTENT_SPACING)
        ) {
            Header(title = stringResource(id = R.string.settings))
            Text(
                text = date,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
internal fun SettingsProfileSection(
    name: String,
    employer: String,
    onNameChange: (String) -> Unit,
    onEmployerChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(space = FORM_GROUP_SPACING)) {
        SettingsTextField(value = name, label = R.string.name, onValueChange = onNameChange)
        SettingsTextField(value = employer, label = R.string.employer, onValueChange = onEmployerChange)
    }
}

@Composable
internal fun SettingsWorkTypeSection(state: SettingsWorkTypeSectionState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)
    ) {
        DropdownMenuBox(
            items = state.workTypes,
            onItemSelected = state.settingsWorkTypeDialogState.onWorkTypeSelected,
            field = DropdownMenuField(
                labelId = R.string.work_type,
                selectedText = state.settingsWorkTypeDialogState.selectedWorkType
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(space = FORM_INLINE_SPACING)) {
            Button(
                onClick = state.settingsWorkTypeDialogState.onAddClick,
                modifier = Modifier.weight(weight = 1f),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.add))
            }
            Button(
                onClick = state.settingsWorkTypeDialogState.onDeleteClick,
                enabled = state.settingsWorkTypeDialogState.selectedWorkType.isNotEmpty() &&
                    state.settingsWorkTypeDialogState.selectedWorkType !in state.protectedWorkTypes,
                modifier = Modifier.weight(weight = 1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(text = stringResource(id = R.string.delete))
            }
        }
    }
}

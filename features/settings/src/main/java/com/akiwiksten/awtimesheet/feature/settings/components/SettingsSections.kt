package com.akiwiksten.awtimesheet.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.akiwiksten.awtimesheet.core.ACTION_BUTTON_FONT_SIZE
import com.akiwiksten.awtimesheet.core.DEFAULT_ELEVATION
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.PADDING_SPACING_SMALL
import com.akiwiksten.awtimesheet.core.ui.AwtButton
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
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
    ) {
        AwtButton(
            onClick = state.onSave,
            enabled = state.isSaveEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.save), fontSize = ACTION_BUTTON_FONT_SIZE)
        }
        AwtButton(
            onClick = state.onGenerateXlsx,
            enabled = state.isReportEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.generate_xlsx), fontSize = ACTION_BUTTON_FONT_SIZE)
        }
        AwtButton(
            onClick = state.onGenerateWorkdaysForMonth,
            enabled = state.isReportEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.generate_workdays_month), fontSize = ACTION_BUTTON_FONT_SIZE)
        }
        AwtButton(
            onClick = state.onGenerateWorkdaysForYear,
            enabled = state.isReportEnabled,
            modifier = Modifier.fillMaxWidth()
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
            .fillMaxWidth(),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = DEFAULT_ELEVATION)
    ) {
        Column(
            modifier = Modifier.padding(all = PADDING_SPACING_SMALL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
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
    Column(verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)) {
        SettingsTextField(value = name, label = R.string.name, onValueChange = onNameChange)
        SettingsTextField(value = employer, label = R.string.employer, onValueChange = onEmployerChange)
    }
}

@Composable
internal fun SettingsWorkTypeSection(state: SettingsWorkTypeSectionState) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)
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
        Row(horizontalArrangement = Arrangement.spacedBy(space = PADDING_SPACING_SMALL)) {
            AwtButton(
                onClick = state.settingsWorkTypeDialogState.onAddClick,
                modifier = Modifier.weight(weight = 1f)
            ) {
                Text(text = stringResource(id = R.string.add))
            }
            AwtButton(
                onClick = state.settingsWorkTypeDialogState.onDeleteClick,
                enabled = state.settingsWorkTypeDialogState.selectedWorkType.isNotEmpty() &&
                    state.settingsWorkTypeDialogState.selectedWorkType !in state.protectedWorkTypes,
                modifier = Modifier.weight(weight = 1f)
            ) {
                Text(text = stringResource(id = R.string.delete))
            }
        }
    }
}

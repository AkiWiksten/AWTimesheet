package com.akiwiksten.awtimesheet.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akiwiksten.awtimesheet.core.FIELD_CORNER_RADIUS
import com.akiwiksten.awtimesheet.core.FORM_MAX_WIDTH
import com.akiwiksten.awtimesheet.core.FORM_SECTION_SPACING
import com.akiwiksten.awtimesheet.feature.settings.R
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsContentBodyState

@Composable
internal fun SettingsCard(title: String? = null, content: @Composable () -> Unit) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = FORM_MAX_WIDTH),
        shape = RoundedCornerShape(size = FIELD_CORNER_RADIUS),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(all = FORM_SECTION_SPACING),
            verticalArrangement = Arrangement.spacedBy(space = FORM_SECTION_SPACING)
        ) {
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()
            }
            content()
        }
    }
}

@Composable
internal fun SettingsGlobalDefaultsCard(state: SettingsContentBodyState) {
    SettingsCard {
        Text(
            text = stringResource(id = R.string.global_defaults),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        HorizontalDivider()

        SettingsDailyWorkTimePickerRow(
            dailyWorkTime = state.uiState.data.dailyWorkTimeEstimate,
            onPickerClick = state.settingsTimePickerState.onDailyWorkTimePickerClick
        )

        SettingsDailyLunchTimeEstimatePickerRow(
            dailyLunchTimeEstimate = state.uiState.data.dailyLunchTimeEstimate,
            onPickerClick = state.settingsTimePickerState.onDailyLunchTimeEstimatePickerClick
        )

        SettingsTextField(
            value = state.uiState.data.initialFlexTimeTotal,
            label = R.string.initial_flex_time_total,
            onValueChange = state.actions.onInitialFlexTimeTotalChange,
            isError = state.settingsSaveUi.isInitialFlexTimeTotalError
        )
    }
}

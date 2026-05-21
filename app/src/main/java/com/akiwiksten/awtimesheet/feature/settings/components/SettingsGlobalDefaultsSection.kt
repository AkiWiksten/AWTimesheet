package com.akiwiksten.awtimesheet.feature.settings.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.feature.settings.SettingsContentBodyState

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



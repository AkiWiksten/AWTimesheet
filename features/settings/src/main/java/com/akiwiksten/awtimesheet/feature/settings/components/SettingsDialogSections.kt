package com.akiwiksten.awtimesheet.feature.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.res.stringResource
import com.akiwiksten.awtimesheet.feature.settings.R
import com.akiwiksten.awtimesheet.core.ui.AddTextFieldDialog
import com.akiwiksten.awtimesheet.core.ui.MyAlertDialog
import com.akiwiksten.awtimesheet.core.ui.TimePickerDialog
import com.akiwiksten.awtimesheet.feature.settings.SettingsTimePickerDialogConfig

@Composable
internal fun SettingsTimePickerDialogsSection(
    workTimePicker: SettingsTimePickerDialogConfig,
    lunchTimePicker: SettingsTimePickerDialogConfig
) {
    SettingsDailyWorkTimePickerDialogSection(
        isVisible = workTimePicker.isVisible,
        currentDailyWorkTime = workTimePicker.time,
        onDismiss = workTimePicker.onDismiss,
        onConfirmed = workTimePicker.onConfirm
    )
    SettingsDailyLunchTimeEstimatePickerDialogSection(
        isVisible = lunchTimePicker.isVisible,
        currentDailyLunchTimeEstimate = lunchTimePicker.time,
        onDismiss = lunchTimePicker.onDismiss,
        onConfirmed = lunchTimePicker.onConfirm
    )
}

@Composable
internal fun SettingsDailyLunchTimeEstimatePickerDialogSection(
    isVisible: Boolean,
    currentDailyLunchTimeEstimate: String,
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    if (isVisible) {
        TimePickerDialog(
            onDismissRequest = onDismiss,
            onConfirmation = onConfirmed,
            titleId = R.string.daily_lunch_time_estimate,
            time = currentDailyLunchTimeEstimate
        )
    }
}

@Composable
internal fun SettingsDailyWorkTimePickerDialogSection(
    isVisible: Boolean,
    currentDailyWorkTime: String,
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    if (isVisible) {
        TimePickerDialog(
            onDismissRequest = onDismiss,
            onConfirmation = onConfirmed,
            titleId = R.string.daily_work_time,
            time = currentDailyWorkTime
        )
    }
}

@Composable
internal fun SettingsAddWorkTypeDialogSection(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirmed: (String) -> Unit
) {
    if (isVisible) {
        AddTextFieldDialog(
            onDismissRequest = onDismiss,
            onConfirmation = onConfirmed,
            label = stringResource(id = R.string.work_type)
        )
    }
}

@Composable
internal fun SettingsRefreshYearConfirmDialogSection(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    if (isVisible) {
        MyAlertDialog(
            onDismissRequest = onDismiss,
            onConfirmation = onConfirmed,
            titleAndText = stringResource(id = R.string.refresh_workdays_year_confirm_title) to
                stringResource(id = R.string.refresh_workdays_year_confirm_message),
            icon = Icons.Default.Warning
        )
    }
}

@Composable
internal fun SettingsGenerateYearConfirmDialogSection(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    if (isVisible) {
        MyAlertDialog(
            onDismissRequest = onDismiss,
            onConfirmation = onConfirmed,
            titleAndText = stringResource(id = R.string.generate_workdays_year_confirm_title) to
                stringResource(id = R.string.generate_workdays_year_confirm_message),
            icon = Icons.Default.Warning
        )
    }
}


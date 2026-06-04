package com.akiwiksten.awtimesheet.feature.settings.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.akiwiksten.awtimesheet.core.DATE_FORMAT
import com.akiwiksten.awtimesheet.core.ui.AddTextFieldDialog
import com.akiwiksten.awtimesheet.core.ui.MyAlertDialog
import com.akiwiksten.awtimesheet.core.ui.TimePickerDialog
import com.akiwiksten.awtimesheet.feature.settings.R
import com.akiwiksten.awtimesheet.feature.settings.model.SettingsTimePickerDialogConfig
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.akiwiksten.awtimesheet.core.R as CoreR

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
internal fun SettingsGenerateMonthConfirmDialogSection(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    if (isVisible) {
        MyAlertDialog(
            onDismissRequest = onDismiss,
            onConfirmation = onConfirmed,
            titleAndText = stringResource(id = R.string.generate_workdays_month_confirm_title) to
                stringResource(id = R.string.generate_workdays_month_confirm_message),
            icon = Icons.Default.Warning
        )
    }
}

@Composable
internal fun SettingsGenerateMonthlyReportConfirmDialogSection(
    isVisible: Boolean,
    selectedDate: String,
    onDismiss: () -> Unit,
    onConfirmed: () -> Unit
) {
    if (isVisible) {
        val monthYear = remember(selectedDate) { formatMonthYear(selectedDate) }
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(imageVector = Icons.Default.Info, contentDescription = null) },
            title = { Text(text = stringResource(id = R.string.generate_xlsx)) },
            text = {
                Text(
                    text = buildAnnotatedString {
                        append("${stringResource(id = R.string.generate_monthly_report_confirm_prefix)} ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(monthYear)
                        }
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmed) {
                    Text(text = stringResource(id = CoreR.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = CoreR.string.dismiss))
                }
            }
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

private fun formatMonthYear(selectedDate: String): String {
    val parser = DateTimeFormatter.ofPattern(DATE_FORMAT)
    val parsedDate = runCatching { LocalDate.parse(selectedDate, parser) }.getOrNull()
    if (parsedDate == null) {
        return selectedDate
    }

    return String.format(Locale.US, "%02d/%04d", parsedDate.monthValue, parsedDate.year)
}

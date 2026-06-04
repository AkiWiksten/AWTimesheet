package com.akiwiksten.awtimesheet.feature.timesheet.workbook.util

import java.time.LocalDate

private const val HOURS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 1440L
private const val MINUTES_PER_VALID_TIME_COMPONENT = 59L
private const val EXCEL_TIME_PRECISION = 15
private const val EXCEL_EPOCH_YEAR = 1899
private const val EXCEL_EPOCH_MONTH = 12
private const val EXCEL_EPOCH_DAY = 30

internal fun String.toMinutesOrNull(): Long? =
    trim()
        .takeIf { it.isNotBlank() }
        ?.split(':')
        ?.takeIf { it.size == 2 }
        ?.let { parts ->
            val hours = parts[0].toLongOrNull()
            val minutes = parts[1].toLongOrNull()
            if (hours != null && minutes != null && minutes in 0..MINUTES_PER_VALID_TIME_COMPONENT) {
                (hours * HOURS_PER_MINUTE) + minutes
            } else {
                null
            }
        }

internal fun Long.toHourMinuteString(): String {
    val hours = this / MINUTES_PER_HOUR
    val minutes = this % MINUTES_PER_HOUR
    return "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}"
}

internal fun Long.toExcelTimeFractionNumberString(): String {
    return java.math.BigDecimal.valueOf(this)
        .divide(
            java.math.BigDecimal.valueOf(MINUTES_PER_DAY),
            EXCEL_TIME_PRECISION,
            java.math.RoundingMode.HALF_UP
        )
        .stripTrailingZeros()
        .toPlainString()
}

internal fun LocalDate.toExcelSerialDate(): Long {
    val excelEpoch = LocalDate.of(EXCEL_EPOCH_YEAR, EXCEL_EPOCH_MONTH, EXCEL_EPOCH_DAY)
    return java.time.temporal.ChronoUnit.DAYS.between(excelEpoch, this)
}

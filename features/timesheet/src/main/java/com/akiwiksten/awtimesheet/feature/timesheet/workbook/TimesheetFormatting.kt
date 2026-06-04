@file:Suppress("MagicNumber", "TooManyFunctions")

package com.akiwiksten.awtimesheet.feature.timesheet.workbook

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.timesheet.entry.PROJECT_SUMMARY_START_COLUMN_INDEX
import com.akiwiksten.awtimesheet.feature.timesheet.model.GenerateTimesheetParams
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetAllowanceSummaryRow
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetAllowanceType
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetEntry
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetLabels
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetWorkTypeSummaryRow
import java.time.LocalDate

private const val HOURS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60L
private const val MINUTES_PER_DAY = 1440L
private const val DAYS_IN_ALPHABET = 26
private const val MINUTES_PER_VALID_TIME_COMPONENT = 59L
private val ALLOWANCE_ORDER = listOf(
    TimesheetAllowanceType.NONE,
    TimesheetAllowanceType.HALF_DAY,
    TimesheetAllowanceType.FULL
)

private fun SingleProjectState.toTimesheetEntry(labels: TimesheetLabels): TimesheetEntry? {
    val parsedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return null
    val normalizedProjectTime = (projectTime.toMinutesOrNull() ?: 0L).toHourMinuteString()
    val isValidEntry = normalizedProjectTime != ZERO_TIME
    return if (isValidEntry) {
        val normalizedProjectName = projectName.trim()
        val normalizedWorkType = workType.trim().ifBlank { labels.defaultWorkTypeLabel }
        val allowanceType = allowance.toAllowanceType(labels)
        TimesheetEntry(
            dayOfMonth = parsedDate.dayOfMonth,
            projectName = normalizedProjectName,
            projectTime = normalizedProjectTime,
            allowanceType = allowanceType,
            allowanceLabel = allowanceType.toExportLabel(labels),
            workType = normalizedWorkType,
            kilometres = kilometres.trim()
        )
    } else {
        null
    }
}

private fun String.toAllowanceType(labels: TimesheetLabels): TimesheetAllowanceType {
    return when (trim()) {
        labels.fullAllowanceSourceLabel -> TimesheetAllowanceType.FULL
        labels.halfDayAllowanceSourceLabel -> TimesheetAllowanceType.HALF_DAY
        else -> TimesheetAllowanceType.NONE
    }
}

private fun TimesheetAllowanceType.toExportLabel(labels: TimesheetLabels): String {
    return when (this) {
        TimesheetAllowanceType.NONE -> labels.noAllowanceExportLabel
        TimesheetAllowanceType.HALF_DAY -> labels.halfDayAllowanceExportLabel
        TimesheetAllowanceType.FULL -> labels.fullAllowanceExportLabel
    }
}

internal fun List<TimesheetEntry>.allDistinctProjectNames(): List<String> {
    return map { it.projectName }
        .filter { it.isNotBlank() }
        .distinct()
}

internal fun List<TimesheetEntry>.allDistinctWorkTypes(): List<String> {
    return map { it.workType }
        .filter { it.isNotBlank() }
        .distinct()
}

internal fun projectSummaryColumnLetters(projectCount: Int): List<String> {
    val startIndex = projectSummaryStartColumnIndex()
    return (0 until projectCount).map { offset -> columnIndexToLetters(startIndex + offset) }
}

internal fun projectSummaryTotalColumnLetters(projectCount: Int): String {
    return columnIndexToLetters(projectSummaryTotalColumnIndex(projectCount))
}

internal fun projectSummaryStartColumnIndex(): Int {
    return PROJECT_SUMMARY_START_COLUMN_INDEX
}

internal fun projectSummaryTotalColumnIndex(projectCount: Int): Int {
    return projectSummaryStartColumnIndex() + projectCount
}

internal fun allowanceLabelColumnIndex(projectCount: Int): Int {
    return projectSummaryTotalColumnIndex(projectCount) + 2
}

internal fun allowanceStartColumnIndex(projectCount: Int): Int {
    return allowanceLabelColumnIndex(projectCount) + 1
}

internal fun allowanceTotalColumnIndex(projectCount: Int): Int {
    return allowanceStartColumnIndex(projectCount) + projectCount
}

internal fun workTypeLabelColumnIndex(projectCount: Int): Int {
    return allowanceTotalColumnIndex(projectCount) + 2
}

internal fun buildCellReference(columnIndex: Int, rowNumber: Int): String {
    return "${columnIndexToLetters(columnIndex)}$rowNumber"
}

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
        .divide(java.math.BigDecimal.valueOf(MINUTES_PER_DAY), 15, java.math.RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
}

internal fun LocalDate.toExcelSerialDate(): Long {
    val excelEpoch = LocalDate.of(1899, 12, 30)
    return java.time.temporal.ChronoUnit.DAYS.between(excelEpoch, this)
}

internal fun dayToColumn(day: Int): String {
    return columnIndexToLetters(day + 1)
}

internal fun columnIndexToLetters(columnIndex: Int): String {
    var value = columnIndex
    val builder = StringBuilder()
    while (value > 0) {
        val remainder = (value - 1) % DAYS_IN_ALPHABET
        builder.insert(0, ('A'.code + remainder).toChar())
        value = (value - 1) / DAYS_IN_ALPHABET
    }
    return builder.toString()
}

internal fun List<SingleProjectState>.toSortedTimesheetEntries(labels: TimesheetLabels): List<TimesheetEntry> {
    return filter { it.projectTime != ZERO_TIME }
        .sortedWith(
            compareBy<SingleProjectState>(
                { it.date },
                { if (it.index >= 0) it.index else Int.MAX_VALUE },
                { it.projectName },
                { it.workType },
                { it.allowance }
            )
        )
        .mapNotNull { it.toTimesheetEntry(labels) }
}

internal fun GenerateTimesheetParams.toTimesheetLabels() = TimesheetLabels(
    defaultWorkTypeLabel = defaultWorkTypeLabel,
    noAllowanceSourceLabel = noAllowanceSourceLabel,
    halfDayAllowanceSourceLabel = halfDayAllowanceSourceLabel,
    fullAllowanceSourceLabel = fullAllowanceSourceLabel,
    noAllowanceExportLabel = noAllowanceExportLabel,
    halfDayAllowanceExportLabel = halfDayAllowanceExportLabel,
    fullAllowanceExportLabel = fullAllowanceExportLabel,
    dayOfMonthLabel = dayOfMonthLabel,
    projectNameLabel = projectNameLabel,
    workTimeByDateLabel = workTimeByDateLabel,
    allowanceLabel = allowanceLabel,
    workTypeLabel = workTypeLabel,
    employerLabel = employerLabel,
    nameLabel = nameLabel,
    totalSumLabel = totalSumLabel,
    startDateLabel = startDateLabel,
    titleLabel = titleLabel,
    endDateLabel = endDateLabel,
    projectTimeLabel = projectTimeLabel
)

internal fun buildAllowanceRows(
    allProjectNames: List<String>,
    labels: TimesheetLabels,
    allowanceCountsByProjectAndType: Map<Pair<String, TimesheetAllowanceType>, Int>,
    allowanceTotalCountsByType: Map<TimesheetAllowanceType, Int>
): List<TimesheetAllowanceSummaryRow> {
    return ALLOWANCE_ORDER.map { allowanceType ->
        TimesheetAllowanceSummaryRow(
            label = allowanceType.toExportLabel(labels),
            countByProjectName = allProjectNames.associateWith { projectName ->
                allowanceCountsByProjectAndType[projectName to allowanceType] ?: 0
            },
            totalCount = allowanceTotalCountsByType[allowanceType] ?: 0
        )
    }
}

internal fun buildWorkTypeRows(
    allProjectNames: List<String>,
    workTypeTimeByProjectAndType: Map<Pair<String, String>, Long>,
    workTypeTotalTime: Map<String, Long>,
    displayedEntriesByDay: Map<Int, List<TimesheetEntry>>
): List<TimesheetWorkTypeSummaryRow> {
    val displayedWorkTypes = displayedEntriesByDay
        .toSortedMap()
        .values
        .flatten()
        .map { it.workType }
        .filter { it.isNotBlank() }
        .distinct()

    return displayedWorkTypes.map { workType ->
        TimesheetWorkTypeSummaryRow(
            label = workType,
            timeByProjectName = allProjectNames.associateWith { projectName ->
                workTypeTimeByProjectAndType[projectName to workType] ?: 0L
            },
            totalTime = workTypeTotalTime[workType] ?: 0L
        )
    }
}

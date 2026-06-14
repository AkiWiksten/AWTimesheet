package com.akiwiksten.awtimesheet.feature.timesheet.mapper

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.timesheet.model.GenerateTimesheetParams
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetAllowanceType
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetEntry
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetLabels
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.toHourMinuteString
import com.akiwiksten.awtimesheet.feature.timesheet.workbook.util.toMinutesOrNull
import java.time.LocalDate

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

internal fun List<SingleProjectState>.toSortedTimesheetEntries(labels: TimesheetLabels): List<TimesheetEntry> {
    return filter { it.projectTime != ZERO_TIME }
        .sortedWith(
            compareBy<SingleProjectState>(
                { it.date },
                { if (it.listIndex >= 0) it.listIndex else Int.MAX_VALUE },
                { it.projectName },
                { it.workType },
                { it.allowance }
            )
        )
        .mapNotNull { it.toTimesheetEntry(labels) }
}

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

internal fun TimesheetAllowanceType.toExportLabel(labels: TimesheetLabels): String {
    return when (this) {
        TimesheetAllowanceType.NONE -> labels.noAllowanceExportLabel
        TimesheetAllowanceType.HALF_DAY -> labels.halfDayAllowanceExportLabel
        TimesheetAllowanceType.FULL -> labels.fullAllowanceExportLabel
    }
}

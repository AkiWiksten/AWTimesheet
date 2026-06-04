package com.akiwiksten.awtimesheet.feature.timesheet.workbook.sections

import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetAllowanceSummaryRow
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetAllowanceType
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetEntry
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetLabels
import com.akiwiksten.awtimesheet.feature.timesheet.model.TimesheetWorkTypeSummaryRow

private val ALLOWANCE_ORDER = listOf(
    TimesheetAllowanceType.NONE,
    TimesheetAllowanceType.HALF_DAY,
    TimesheetAllowanceType.FULL
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

internal fun TimesheetAllowanceType.toExportLabel(labels: TimesheetLabels): String {
    return when (this) {
        TimesheetAllowanceType.NONE -> labels.noAllowanceExportLabel
        TimesheetAllowanceType.HALF_DAY -> labels.halfDayAllowanceExportLabel
        TimesheetAllowanceType.FULL -> labels.fullAllowanceExportLabel
    }
}

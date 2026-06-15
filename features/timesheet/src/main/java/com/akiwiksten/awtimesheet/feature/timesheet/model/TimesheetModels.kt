package com.akiwiksten.awtimesheet.feature.timesheet.model

import android.content.Context
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import java.time.LocalDate

// Input params.
data class GenerateTimesheetParams(
    val ctx: Context,
    val projectsByMonth: List<SingleProjectState>,
    val endOfMonthDate: String,
    val name: String,
    val employer: String,
    val defaultWorkTypeLabel: String,
    val noAllowanceSourceLabel: String,
    val halfDayAllowanceSourceLabel: String,
    val fullAllowanceSourceLabel: String,
    val noAllowanceExportLabel: String,
    val halfDayAllowanceExportLabel: String,
    val fullAllowanceExportLabel: String,
    val dayOfMonthLabel: String = "Day of Month",
    val projectNameLabel: String = "Project name",
    val workTimeByDateLabel: String = "Work time by date",
    val allowanceLabel: String = "Allowance",
    val workTypeLabel: String = "Work type",
    val commentLabel: String = "Comment",
    val employerLabel: String = "Employer",
    val nameLabel: String = "Name",
    val totalSumLabel: String = "TOTAL SUM",
    val startDateLabel: String = "Start date",
    val titleLabel: String = "Timesheet",
    val endDateLabel: String = "End date",
    val projectTimeLabel: String = "Project time",
    val totalLabel: String,
    val generalLabel: String,
    val workTimeTotalLabel: String,
    val kilometresLabel: String,
    val flexTimeTotalLabel: String,
    val totalFlexTimeTotal: String = ZERO_TIME
)

internal data class TimesheetEntryAggregates(
    val summaryProjectTimes: Map<String, Long>,
    val summaryProjectKilometres: Map<String, Long>,
    val allowanceCountsByProjectAndType: Map<Pair<String, TimesheetAllowanceType>, Int>,
    val allowanceTotalCountsByType: Map<TimesheetAllowanceType, Int>,
    val workTypeTimeByProjectAndType: Map<Pair<String, String>, Long>,
    val workTypeTotalTime: Map<String, Long>,
    val totalWorkTime: Long,
    val totalKilometres: Long
)

internal data class TimesheetDisplayData(
    val displayedEntriesByDay: Map<Int, List<TimesheetEntry>>,
    val overflowedDays: List<Int>
)

internal data class TimesheetExportData(
    val name: String,
    val employer: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalLabel: String,
    val generalLabel: String,
    val workTimeTotalLabel: String,
    val kilometresLabel: String,
    val summaryProjectNames: List<String>,
    val summaryProjectTimes: Map<String, Long>,
    val summaryProjectKilometres: Map<String, Long>,
    val totalWorkTime: Long,
    val totalKilometres: Long,
    val dayOfMonthLabel: String,
    val projectNameLabel: String,
    val workTimeByDateLabel: String,
    val allowanceLabel: String,
    val workTypeLabel: String,
    val commentLabel: String,
    val employerLabel: String,
    val nameLabel: String,
    val totalSumLabel: String,
    val startDateLabel: String,
    val titleLabel: String,
    val endDateLabel: String,
    val projectTimeLabel: String,
    val allowanceRows: List<TimesheetAllowanceSummaryRow>,
    val workTypeRows: List<TimesheetWorkTypeSummaryRow>,
    val displayedEntriesByDay: Map<Int, List<TimesheetEntry>>,
    val overflowedDays: List<Int>,
    val hiddenProjectNames: List<String>,
    val hiddenWorkTypes: List<String>,
    val flexTimeTotalLabel: String,
    val totalFlexTimeTotal: String
)

internal data class TimesheetAllowanceSummaryRow(
    val label: String,
    val countByProjectName: Map<String, Int>,
    val totalCount: Int
)

internal data class TimesheetWorkTypeSummaryRow(
    val label: String,
    val timeByProjectName: Map<String, Long>,
    val totalTime: Long
)

internal data class TimesheetEntry(
    val dayOfMonth: Int,
    val projectName: String,
    val projectTime: String,
    val allowanceType: TimesheetAllowanceType,
    val allowanceLabel: String,
    val workType: String,
    val comment: String,
    val kilometres: String
)

internal data class TimesheetLabels(
    val defaultWorkTypeLabel: String,
    val noAllowanceSourceLabel: String,
    val halfDayAllowanceSourceLabel: String,
    val fullAllowanceSourceLabel: String,
    val noAllowanceExportLabel: String,
    val halfDayAllowanceExportLabel: String,
    val fullAllowanceExportLabel: String,
    val dayOfMonthLabel: String,
    val projectNameLabel: String,
    val workTimeByDateLabel: String,
    val allowanceLabel: String,
    val workTypeLabel: String,
    val commentLabel: String,
    val employerLabel: String,
    val nameLabel: String,
    val totalSumLabel: String,
    val startDateLabel: String,
    val titleLabel: String,
    val endDateLabel: String,
    val projectTimeLabel: String
)

internal enum class TimesheetAllowanceType {
    NONE,
    HALF_DAY,
    FULL
}

package com.akiwiksten.awtimesheet.feature.settings

import android.content.Context
import com.akiwiksten.awtimesheet.feature.timesheet.entry.TimesheetGeneratorEntry
import com.akiwiksten.awtimesheet.feature.timesheet.model.GenerateTimesheetParams
import com.akiwiksten.awtimesheet.core.R as CoreR
import com.akiwiksten.awtimesheet.feature.timesheet.R as TimesheetR

internal fun generateTimesheetReport(
    ctx: Context,
    event: SettingsEvent.TimesheetReportReady
) {
    TimesheetGeneratorEntry.generateXlsx(
        params = ctx.createTimesheetParams(event)
    )
}

private fun Context.createTimesheetParams(
    event: SettingsEvent.TimesheetReportReady
) = GenerateTimesheetParams(
    ctx = this,
    projectsByMonth = event.projectsByMonth,
    endOfMonthDate = event.endOfMonthDate,
    name = event.name,
    employer = event.employer,
    defaultWorkTypeLabel = getString(CoreR.string.other),
    noAllowanceSourceLabel = getString(CoreR.string.no_allowance),
    halfDayAllowanceSourceLabel = getString(CoreR.string.half_day_allowance),
    fullAllowanceSourceLabel = getString(CoreR.string.full_allowance),
    noAllowanceExportLabel = getString(TimesheetR.string.timesheet_no_allowance_short),
    halfDayAllowanceExportLabel = getString(TimesheetR.string.timesheet_half_day_allowance_short),
    fullAllowanceExportLabel = getString(TimesheetR.string.timesheet_full_allowance_short),
    dayOfMonthLabel = getString(TimesheetR.string.timesheet_day_of_month),
    projectNameLabel = getString(CoreR.string.project_name),
    workTimeByDateLabel = getString(TimesheetR.string.work_time_by_date),
    allowanceLabel = getString(CoreR.string.allowance),
    workTypeLabel = getString(CoreR.string.work_type),
    commentLabel = getString(CoreR.string.comment),
    employerLabel = getString(CoreR.string.employer),
    nameLabel = getString(CoreR.string.name),
    totalSumLabel = getString(TimesheetR.string.total_sum),
    startDateLabel = getString(TimesheetR.string.timesheet_start_date),
    titleLabel = getString(TimesheetR.string.timesheet_title),
    endDateLabel = getString(TimesheetR.string.timesheet_end_date),
    projectTimeLabel = getString(CoreR.string.project_time),
    totalLabel = getString(TimesheetR.string.timesheet_total),
    generalLabel = getString(TimesheetR.string.timesheet_general),
    workTimeTotalLabel = getString(TimesheetR.string.timesheet_work_time_total),
    kilometresLabel = getString(CoreR.string.kilometres),
    flexTimeTotalLabel = getString(TimesheetR.string.timesheet_flex_time_total),
    totalFlexTimeTotal = event.totalFlexTimeTotal
)

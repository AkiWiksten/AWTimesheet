package com.akiwiksten.awtimesheet.feature.settings

import android.content.Context
import com.akiwiksten.awtimesheet.feature.timesheet.entry.TimesheetGeneratorEntry
import com.akiwiksten.awtimesheet.feature.timesheet.model.GenerateTimesheetParams

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
    defaultWorkTypeLabel = getString(R.string.other),
    noAllowanceSourceLabel = getString(R.string.no_allowance),
    halfDayAllowanceSourceLabel = getString(R.string.half_day_allowance),
    fullAllowanceSourceLabel = getString(R.string.full_allowance),
    noAllowanceExportLabel = getString(R.string.timesheet_no_allowance_short),
    halfDayAllowanceExportLabel = getString(R.string.timesheet_half_day_allowance_short),
    fullAllowanceExportLabel = getString(R.string.timesheet_full_allowance_short),
    dayOfMonthLabel = getString(R.string.timesheet_day_of_month),
    projectNameLabel = getString(R.string.project_name),
    workTimeByDateLabel = getString(R.string.work_time_by_date),
    allowanceLabel = getString(R.string.allowance),
    workTypeLabel = getString(R.string.work_type),
    employerLabel = getString(R.string.employer),
    nameLabel = getString(R.string.name),
    totalSumLabel = getString(R.string.total_sum),
    startDateLabel = getString(R.string.timesheet_start_date),
    titleLabel = getString(R.string.timesheet_title),
    endDateLabel = getString(R.string.timesheet_end_date),
    projectTimeLabel = getString(R.string.project_time),
    totalLabel = getString(R.string.timesheet_total),
    generalLabel = getString(R.string.timesheet_general),
    workTimeTotalLabel = getString(R.string.timesheet_work_time_total),
    kilometresLabel = getString(R.string.kilometres),
    flexTimeTotalLabel = getString(R.string.timesheet_flex_time_total),
    totalFlexTimeTotal = event.totalFlexTimeTotal
)

package com.akiwiksten.awtimesheet.feature.settings

import android.content.Context
import com.akiwiksten.awtimesheet.feature.settings.R
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.GenerateTimesheetParams
import com.akiwiksten.awtimesheet.feature.settings.timesheet.entry.TimesheetGeneratorEntry

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
    totalLabel = getString(R.string.timesheet_total),
    generalLabel = getString(R.string.timesheet_general),
    workTimeTotalLabel = getString(R.string.timesheet_work_time_total),
    kilometresLabel = getString(R.string.kilometres),
    flexTimeTotalLabel = getString(R.string.timesheet_flex_time_total),
    totalFlexTimeTotal = event.totalFlexTimeTotal
)

package com.akiwiksten.awtimesheet.feature.settings

import android.content.Context
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.settings.report.GeneratePdfParams
import com.akiwiksten.awtimesheet.feature.settings.report.GenerateTimesheetParams
import com.akiwiksten.awtimesheet.feature.settings.report.MonthlyReportGenerator
import com.akiwiksten.awtimesheet.feature.settings.report.TimesheetGenerator

internal fun generatePdfReport(
    ctx: Context,
    projectsByMonth: List<SingleProjectState>,
    endOfMonthDate: String,
    name: String,
    employer: String
) {
    val titles = listOf(
        R.string.date,
        R.string.project,
        R.string.work_time_by_date,
        R.string.allowance,
        R.string.work_type,
        R.string.kilometres
    ).map { ctx.getString(it) }

    MonthlyReportGenerator.generatePdf(
        params = GeneratePdfParams(
            ctx = ctx,
            projectsByMonth = projectsByMonth,
            endOfMonthDate = endOfMonthDate,
            totalSumLabel = ctx.getString(R.string.total_sum),
            monthlyReportLabel = ctx.getString(R.string.monthly_report),
            name = name,
            employer = employer,
            projectTitles = titles
        )
    )
}

internal fun generateTimesheetReport(
    ctx: Context,
    projectsByMonth: List<SingleProjectState>,
    endOfMonthDate: String,
    name: String,
    employer: String,
    totalFlexTimeTotal: String = "00:00"
) {
    TimesheetGenerator.generateXlsx(
        params = ctx.createTimesheetParams(
            projectsByMonth = projectsByMonth,
            endOfMonthDate = endOfMonthDate,
            name = name,
            employer = employer,
            totalFlexTimeTotal = totalFlexTimeTotal
        )
    )
}

private fun Context.createTimesheetParams(
    projectsByMonth: List<SingleProjectState>,
    endOfMonthDate: String,
    name: String,
    employer: String,
    totalFlexTimeTotal: String = "00:00"
) = GenerateTimesheetParams(
    ctx = this,
    projectsByMonth = projectsByMonth,
    endOfMonthDate = endOfMonthDate,
    name = name,
    employer = employer,
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
    totalFlexTimeTotal = totalFlexTimeTotal
)

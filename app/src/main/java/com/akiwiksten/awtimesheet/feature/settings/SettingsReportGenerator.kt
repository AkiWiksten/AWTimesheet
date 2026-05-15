package com.akiwiksten.awtimesheet.feature.settings

import android.content.Context
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.settings.report.GeneratePdfParams
import com.akiwiksten.awtimesheet.feature.settings.report.MonthlyReportGenerator

internal fun generateReport(
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

package com.akiwiksten.worktime30.feature.settings

import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.feature.settings.report.GeneratePdfParams
import com.akiwiksten.worktime30.feature.settings.report.MonthlyReportGenerator

internal fun generateReport(params: GenerateReportParams) {
    val titles = listOf(
        R.string.date,
        R.string.project,
        R.string.work_time_today,
        R.string.allowance,
        R.string.work_type,
        R.string.kilometres
    ).map { params.ctx.getString(it) }

    MonthlyReportGenerator.generatePdf(
        params = GeneratePdfParams(
            ctx = params.ctx,
            projectsByMonth = params.projectsByMonth,
            endOfMonthDate = params.endOfMonthDate,
            totalSumLabel = params.ctx.getString(R.string.total_sum),
            monthlyReportLabel = params.ctx.getString(R.string.monthly_report),
            name = params.name,
            employer = params.employer,
            projectTitles = titles
        )
    )
}

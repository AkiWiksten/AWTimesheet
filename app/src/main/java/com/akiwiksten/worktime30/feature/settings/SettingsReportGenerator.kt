package com.akiwiksten.worktime30.feature.settings

import android.content.Context
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.GeneratePdfParams
import com.akiwiksten.worktime30.core.MonthlyReportGenerator
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity

internal fun generateReport(
    ctx: Context,
    projectsByMonth: List<ProjectEntity>,
    endOfMonthDate: String,
    name: String,
    employer: String
) {
    val titles = listOf(
        R.string.date,
        R.string.project,
        R.string.work_time_today,
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

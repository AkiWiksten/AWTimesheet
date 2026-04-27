package com.akiwiksten.worktime30.feature.settings

import android.content.Context
import com.akiwiksten.worktime30.domain.model.SingleProjectState

internal data class GenerateReportParams(
    val ctx: Context,
    val projectsByMonth: List<SingleProjectState>,
    val endOfMonthDate: String,
    val name: String,
    val employer: String
)

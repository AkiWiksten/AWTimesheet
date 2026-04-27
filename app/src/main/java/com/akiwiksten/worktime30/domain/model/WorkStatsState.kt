package com.akiwiksten.worktime30.domain.model

import com.akiwiksten.worktime30.core.ZERO_TIME

data class WorkStatsState(
    val dailyWorkTimeEstimate: String = ZERO_TIME,
    val dailyLunchTimeEstimate: String = ZERO_TIME,
    val initialFlexTimeTotal: String = ZERO_TIME
)

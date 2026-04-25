package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState

fun WorkdayEntity.toWorkStatsState(lunchTime: String, initialFlexTimeTotal: String): WorkStatsState {
    return WorkStatsState(
        dailyWorkTime = workTimeTodayEstimate,
        lunchTime = lunchTime,
        initialFlexTimeTotal = initialFlexTimeTotal
    )
}

fun WorkStatsState.toWorkdayEntity(date: String, workTimeToday: String): WorkdayEntity {
    return WorkdayEntity(
        date = date,
        workTimeToday = workTimeToday.ifEmpty { ZERO_TIME },
        workTimeTodayEstimate = dailyWorkTime
    )
}

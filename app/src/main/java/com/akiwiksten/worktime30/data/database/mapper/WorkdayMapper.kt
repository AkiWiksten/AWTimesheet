package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState

fun WorkdayEntity.toWorkStatsState(dailyLunchTimeEstimate: String, initialFlexTimeTotal: String): WorkStatsState {
    return WorkStatsState(
        dailyWorkTimeEstimate = workTimeTodayEstimate,
        dailyLunchTimeEstimate = dailyLunchTimeEstimate,
        initialFlexTimeTotal = initialFlexTimeTotal
    )
}

fun WorkStatsState.toWorkdayEntity(date: String, workTimeToday: String): WorkdayEntity {
    return WorkdayEntity(
        date = date,
        workTimeToday = workTimeToday.ifEmpty { ZERO_TIME },
        workTimeTodayEstimate = dailyWorkTimeEstimate
    )
}

package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.domain.model.WorkStatsState

fun WorkdayEntity.toWorkStatsState(dailyLunchTimeEstimate: String, initialFlexTimeTotal: String): WorkStatsState {
    return WorkStatsState(
        dailyWorkTimeEstimate = workTimeTodayEstimate,
        dailyLunchTimeEstimate = dailyLunchTimeEstimate,
        initialFlexTimeTotal = initialFlexTimeTotal
    )
}

fun WorkStatsState.toWorkdayEntity(date: String): WorkdayEntity {
    return WorkdayEntity(
        date = date,
        workTimeTodayEstimate = dailyWorkTimeEstimate
    )
}

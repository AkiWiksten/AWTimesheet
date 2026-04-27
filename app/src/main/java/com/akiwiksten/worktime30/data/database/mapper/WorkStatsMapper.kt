package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.domain.model.WorkStatsState

fun WorkStatsEntity.toDomain(): WorkStatsState {
    return WorkStatsState(
        dailyWorkTimeEstimate = dailyWorkTimeEstimate,
        dailyLunchTimeEstimate = dailyLunchTimeEstimate,
        initialFlexTimeTotal = initialFlexTimeTotal
    )
}

fun WorkStatsState.toEntity(): WorkStatsEntity {
    return WorkStatsEntity(
        dailyWorkTimeEstimate = dailyWorkTimeEstimate,
        dailyLunchTimeEstimate = dailyLunchTimeEstimate,
        initialFlexTimeTotal = initialFlexTimeTotal
    )
}

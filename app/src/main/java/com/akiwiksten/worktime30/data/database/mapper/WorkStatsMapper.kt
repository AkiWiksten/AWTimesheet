package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState

fun WorkStatsEntity.toDomain(): WorkStatsState {
    return WorkStatsState(
        dailyWorkTime = dailyWorkTime,
        lunchTime = lunchTime,
        balanceTotal = balanceTotal
    )
}

fun WorkStatsState.toEntity(): WorkStatsEntity {
    return WorkStatsEntity(
        dailyWorkTime = dailyWorkTime,
        lunchTime = lunchTime,
        balanceTotal = balanceTotal
    )
}

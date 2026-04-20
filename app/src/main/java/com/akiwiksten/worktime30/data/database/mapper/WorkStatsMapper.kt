package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.feature.projects.single.details.WorkStatsState

fun WorkStatsEntity.toDomain(): WorkStatsState {
    return WorkStatsState(
        dailyWorkTime = dailyWorkTime,
        lunchTime = lunchTime,
        workTimeTotal = workTimeTotal,
        balanceTotal = balanceTotal
    )
}

fun WorkStatsState.toEntity(): WorkStatsEntity {
    return WorkStatsEntity(
        dailyWorkTime = dailyWorkTime,
        lunchTime = lunchTime,
        workTimeTotal = workTimeTotal,
        balanceTotal = balanceTotal
    )
}

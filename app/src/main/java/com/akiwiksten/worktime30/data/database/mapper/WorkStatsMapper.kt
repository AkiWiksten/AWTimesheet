package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import com.akiwiksten.worktime30.domain.model.WorkStatsState

fun SettingsEntity.toWorkStatsDomain(): WorkStatsState {
    return WorkStatsState(
        dailyWorkTimeEstimate = dailyWorkTimeEstimate,
        dailyLunchTimeEstimate = dailyLunchTimeEstimate,
        initialFlexTimeTotal = initialFlexTimeTotal
    )
}

fun WorkStatsState.mergeIntoSettings(existing: SettingsEntity?): SettingsEntity {
    return SettingsEntity(
        id = 1,
        name = existing?.name.orEmpty(),
        employer = existing?.employer.orEmpty(),
        dailyWorkTimeEstimate = dailyWorkTimeEstimate,
        dailyLunchTimeEstimate = dailyLunchTimeEstimate,
        initialFlexTimeTotal = initialFlexTimeTotal
    )
}

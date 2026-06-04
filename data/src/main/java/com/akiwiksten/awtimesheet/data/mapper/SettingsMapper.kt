package com.akiwiksten.awtimesheet.data.mapper

import com.akiwiksten.awtimesheet.data.database.entity.SettingsEntity
import com.akiwiksten.awtimesheet.domain.model.SettingsState

fun SettingsEntity.toDomain(): SettingsState {
    return SettingsState(
        name = name,
        employer = employer,
        dailyWorkTimeEstimate = dailyWorkTimeEstimate,
        dailyLunchTimeEstimate = dailyLunchTimeEstimate,
        initialFlexTimeTotal = initialFlexTimeTotal,
    )
}

fun SettingsState.toEntity(): SettingsEntity {
    return SettingsEntity(
        name = name,
        employer = employer,
        dailyWorkTimeEstimate = dailyWorkTimeEstimate,
        dailyLunchTimeEstimate = dailyLunchTimeEstimate,
        initialFlexTimeTotal = initialFlexTimeTotal,
    )
}

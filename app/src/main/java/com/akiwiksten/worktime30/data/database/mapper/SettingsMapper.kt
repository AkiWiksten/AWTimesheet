package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import com.akiwiksten.worktime30.feature.settings.SettingsState

fun SettingsEntity.toDomain(): SettingsState {
    return SettingsState(
        name = name,
        employer = employer,
    )
}

fun SettingsState.toEntity(): SettingsEntity {
    return SettingsEntity(
        name = name,
        employer = employer,
    )
}

package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.domain.model.SettingsState

fun WorkdayEntity.toSettingsState(dailyLunchTimeEstimate: String, initialFlexTimeTotal: String): SettingsState {
    return SettingsState(
        dailyWorkTimeEstimate = workTimeTodayEstimate,
        dailyLunchTimeEstimate = dailyLunchTimeEstimate,
        initialFlexTimeTotal = initialFlexTimeTotal
    )
}

fun SettingsState.toWorkdayEntity(date: String): WorkdayEntity {
    return WorkdayEntity(
        date = date,
        workTimeTodayEstimate = dailyWorkTimeEstimate
    )
}

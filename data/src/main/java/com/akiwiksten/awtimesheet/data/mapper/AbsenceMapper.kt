package com.akiwiksten.awtimesheet.data.mapper

import com.akiwiksten.awtimesheet.data.database.entity.AbsenceEntity
import com.akiwiksten.awtimesheet.domain.model.AbsenceState

fun AbsenceEntity.toDomain(): AbsenceState {
    return AbsenceState(
        id = id,
        absenceType = absenceType,
        startDate = startDate,
        endDate = endDate,
        hasWeekends = hasWeekends
    )
}

fun AbsenceState.toEntity(): AbsenceEntity {
    return AbsenceEntity(
        id = id,
        absenceType = absenceType,
        startDate = startDate,
        endDate = endDate,
        hasWeekends = hasWeekends
    )
}

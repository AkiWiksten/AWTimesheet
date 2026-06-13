package com.akiwiksten.awtimesheet.data.mapper

import com.akiwiksten.awtimesheet.data.database.entity.AbsenceEntity
import com.akiwiksten.awtimesheet.domain.model.AbsenceState

fun AbsenceEntity.toDomain(): AbsenceState {
    return AbsenceState(
        absenceType = absenceType,
        startDate = startDate,
        endDate = endDate,
    )
}

fun AbsenceState.toEntity(): AbsenceEntity {
    return AbsenceEntity(
        absenceType = absenceType,
        startDate = startDate,
        endDate = endDate,
    )
}

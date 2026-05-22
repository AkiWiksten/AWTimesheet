package com.akiwiksten.awtimesheet.domain.mapper

import com.akiwiksten.awtimesheet.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState

fun ProjectDetailsEntity.toDomain(): ProjectDetailsState {
    return ProjectDetailsState(
        date = date,
        projectName = projectName,
        startTime = startTime,
        endTime = endTime,
        lunchStart = lunchStart,
        lunchEnd = lunchEnd,
        breakStart = breakStart,
        breakEnd = breakEnd,
        projectTime = projectTime,
        lunchTimeEstimate = lunchTimeEstimate,
    )
}

fun ProjectDetailsState.toEntity(): ProjectDetailsEntity {
    return ProjectDetailsEntity(
        date = date,
        projectName = projectName,
        startTime = startTime,
        endTime = endTime,
        lunchStart = lunchStart,
        lunchEnd = lunchEnd,
        breakStart = breakStart,
        breakEnd = breakEnd,
        projectTime = projectTime,
        lunchTimeEstimate = lunchTimeEstimate,
    )
}

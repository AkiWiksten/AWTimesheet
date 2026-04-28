package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState

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

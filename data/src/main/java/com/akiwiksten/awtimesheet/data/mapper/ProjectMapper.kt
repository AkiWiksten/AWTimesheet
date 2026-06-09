package com.akiwiksten.awtimesheet.data.mapper

import com.akiwiksten.awtimesheet.data.database.entity.ProjectEntity
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState

fun ProjectEntity.toDomain(): SingleProjectState {
    return SingleProjectState(
        date = date,
        listIndex = -1,
        projectName = projectName,
        projectTime = projectTime,
        kilometres = kilometres.toString(),
        allowance = allowance,
        workType = workType,
    )
}

fun SingleProjectState.toEntity(): ProjectEntity {
    return ProjectEntity(
        date = date,
        projectName = projectName,
        projectTime = projectTime,
        kilometres = kilometres.toIntOrNull() ?: 0,
        allowance = allowance,
        workType = workType,
    )
}

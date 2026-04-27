package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.domain.model.SingleProjectState

fun ProjectEntity.toDomain(): SingleProjectState {
    return SingleProjectState(
        date = date,
        index = -1,
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

package com.akiwiksten.awtimesheet.data.mapper

import com.akiwiksten.awtimesheet.data.database.entity.ProjectEntity
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState

fun ProjectEntity.toDomain(): SingleProjectState {
    return SingleProjectState(
        date = date,
        listIndex = 0, // Placeholder, will be updated if needed in UI
        isAddMode = false,
        projectName = projectName,
        projectTime = projectTime,
        kilometres = if (kilometres == 0) "" else kilometres.toString(),
        allowance = allowance,
        workType = workType,
        comment = comment,
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
        comment = comment,
    )
}

package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState

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

package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity

fun ProjectNameEntity.toDomain(): String {
    return name
}

fun String.toEntity(): ProjectNameEntity {
    return ProjectNameEntity(
        name = this
    )
}

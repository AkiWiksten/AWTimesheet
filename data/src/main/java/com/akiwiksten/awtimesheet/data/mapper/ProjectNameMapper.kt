package com.akiwiksten.awtimesheet.data.mapper

import com.akiwiksten.awtimesheet.data.database.entity.ProjectNameEntity

fun ProjectNameEntity.toDomain(): String {
    return name
}

fun String.toProjectNameEntity(): ProjectNameEntity {
    return ProjectNameEntity(
        name = this
    )
}

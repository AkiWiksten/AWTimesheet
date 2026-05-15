package com.akiwiksten.awtimesheet.data.database.mapper

import com.akiwiksten.awtimesheet.data.database.entity.WorkTypeEntity

fun WorkTypeEntity.toDomain(): String {
    return workType
}

fun String.toWorkTypeEntity(): WorkTypeEntity {
    return WorkTypeEntity(
        workType = this
    )
}

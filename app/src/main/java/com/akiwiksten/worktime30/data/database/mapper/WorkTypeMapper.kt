package com.akiwiksten.worktime30.data.database.mapper

import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity

fun WorkTypeEntity.toDomain(): String {
    return workType
}

fun String.toWorkTypeEntity(): WorkTypeEntity {
    return WorkTypeEntity(
        workType = this
    )
}

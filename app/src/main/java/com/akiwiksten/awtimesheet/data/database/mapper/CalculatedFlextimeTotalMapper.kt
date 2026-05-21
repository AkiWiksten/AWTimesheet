package com.akiwiksten.awtimesheet.data.database.mapper

import com.akiwiksten.awtimesheet.data.database.entity.CalculatedFlextimeTotalEntity
import com.akiwiksten.awtimesheet.data.database.entity.WorkdayEntity

fun CalculatedFlextimeTotalEntity.toCalculatedFlextimeTotal(): String {
    return calculatedFlexTimeTotal
}

fun String.toCalculatedFlextimeTotalEntity(): CalculatedFlextimeTotalEntity {
    return CalculatedFlextimeTotalEntity(
        id = 1,
        calculatedFlexTimeTotal = this
    )
}

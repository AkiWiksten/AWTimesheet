package com.akiwiksten.awtimesheet.data.mapper

import com.akiwiksten.awtimesheet.data.database.entity.CalculatedFlextimeTotalEntity

fun CalculatedFlextimeTotalEntity.toCalculatedFlextimeTotaDomain(): String {
    return calculatedFlexTimeTotal
}

fun String.toCalculatedFlextimeTotalEntity(): CalculatedFlextimeTotalEntity {
    return CalculatedFlextimeTotalEntity(
        id = 1,
        calculatedFlexTimeTotal = this
    )
}


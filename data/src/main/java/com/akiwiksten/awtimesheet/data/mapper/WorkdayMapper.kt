package com.akiwiksten.awtimesheet.data.mapper

import com.akiwiksten.awtimesheet.data.database.entity.WorkdayEntity

fun WorkdayEntity.toWorkTimeByDateEstimate(): String {
    return workTimeByDateEstimate
}

fun String.toWorkdayEntity(date: String): WorkdayEntity {
    return WorkdayEntity(
        date = date,
        workTimeByDateEstimate = this
    )
}


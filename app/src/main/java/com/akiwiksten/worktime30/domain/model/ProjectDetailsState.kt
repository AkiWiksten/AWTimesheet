package com.akiwiksten.worktime30.domain.model

import com.akiwiksten.worktime30.core.ZERO_TIME

data class ProjectDetailsState(
    val date: String = "",
    val projectName: String = "",
    val startTime: String = ZERO_TIME,
    val endTime: String = ZERO_TIME,
    val lunchStart: String = ZERO_TIME,
    val lunchEnd: String = ZERO_TIME,
    val breakStart: String = ZERO_TIME,
    val breakEnd: String = ZERO_TIME,
    val projectTime: String = ZERO_TIME,
    val lunchTimeEstimate: String = ZERO_TIME
)

fun ProjectDetailsState.isNewDayForProject(): Boolean {
    fun isZero(time: String) = time == ZERO_TIME || time.isEmpty()

    return isZero(startTime) &&
        isZero(endTime) &&
        isZero(projectTime) &&
        isZero(lunchStart) &&
        isZero(lunchEnd) &&
        isZero(breakStart) &&
        isZero(breakEnd)
}

fun ProjectDetailsState.hasOnlyProjectTime(): Boolean {
    fun isZero(time: String) = time == ZERO_TIME || time.isEmpty()

    return isZero(startTime) &&
        isZero(endTime) &&
        !isZero(projectTime) &&
        isZero(lunchStart) &&
        isZero(lunchEnd) &&
        isZero(breakStart) &&
        isZero(breakEnd)
}

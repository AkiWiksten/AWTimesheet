package com.akiwiksten.awtimesheet.domain.model

import android.os.Parcelable
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import kotlinx.parcelize.Parcelize

@Parcelize
data class ProjectDetailsState(
    val date: String = "",
    val projectName: String = "",
    val originalProjectName: String = "",
    val startTime: String = ZERO_TIME,
    val endTime: String = ZERO_TIME,
    val lunchStart: String = ZERO_TIME,
    val lunchEnd: String = ZERO_TIME,
    val breakStart: String = ZERO_TIME,
    val breakEnd: String = ZERO_TIME,
    val projectTime: String = ZERO_TIME,
    val lunchTimeEstimate: String = ZERO_TIME
) : Parcelable

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

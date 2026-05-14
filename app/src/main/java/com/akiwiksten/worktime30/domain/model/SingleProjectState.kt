package com.akiwiksten.worktime30.domain.model

import android.os.Parcelable
import com.akiwiksten.worktime30.core.ZERO_TIME
import kotlinx.parcelize.Parcelize

@Parcelize
data class SingleProjectState(
    val index: Int = -1,
    val projectName: String = "",
    val projectTime: String = ZERO_TIME,
    val kilometres: String = "0",
    val allowance: String = "",
    val workType: String = "",
    val date: String = ""
) : Parcelable

fun SingleProjectState.isProjectNameOnlyPlaceholder(): Boolean {
    return date.isBlank() &&
        projectTime == ZERO_TIME &&
        kilometres == "0" &&
        allowance.isBlank() &&
        workType.isBlank()
}

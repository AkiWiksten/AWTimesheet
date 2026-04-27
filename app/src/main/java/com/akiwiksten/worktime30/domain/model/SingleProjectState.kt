package com.akiwiksten.worktime30.domain.model

import com.akiwiksten.worktime30.core.ZERO_TIME

data class SingleProjectState(
    val index: Int = -1,
    val projectName: String = "",
    val projectTime: String = ZERO_TIME,
    val kilometres: String = "0",
    val allowance: String = "",
    val workType: String = "",
    val projectDetails: ProjectDetailsState? = null,
    val workStats: WorkStatsState? = null,
    val date: String = ""
)

fun SingleProjectState.isProjectNameOnlyPlaceholder(): Boolean {
    return date.isBlank() &&
        projectTime == ZERO_TIME &&
        kilometres == "0" &&
        allowance.isBlank() &&
        workType.isBlank()
}

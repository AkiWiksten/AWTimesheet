package com.akiwiksten.worktime30.domain.model

import com.akiwiksten.worktime30.core.ZERO_TIME

data class SettingsState(
    val name: String = "",
    val employer: String = "",
    val dailyWorkTimeEstimate: String = "",
    val lunchTimeEstimate: String = ZERO_TIME,
    val selectedDate: String = "",
    val endMonthDate: String = "",
    val workTypes: List<String> = emptyList(),
    val projectsByMonth: List<SingleProjectState> = emptyList()
)

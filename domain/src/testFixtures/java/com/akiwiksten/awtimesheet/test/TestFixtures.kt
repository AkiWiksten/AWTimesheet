@file:Suppress("unused", "LongParameterList")

package com.akiwiksten.awtimesheet.test

import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.WorkdayStatsRow

fun settingsState(
    name: String = "",
    employer: String = "",
    dailyWorkTimeEstimate: String = "00:00",
    dailyLunchTimeEstimate: String = "00:00",
    initialFlexTimeTotal: String = "00:00",
    calculatedFlexTimeTotal: String = "00:00",
    workTypes: List<String> = emptyList(),
): SettingsState {
    return SettingsState(
        name = name,
        employer = employer,
        dailyWorkTimeEstimate = dailyWorkTimeEstimate,
        dailyLunchTimeEstimate = dailyLunchTimeEstimate,
        initialFlexTimeTotal = initialFlexTimeTotal,
        calculatedFlexTimeTotal = calculatedFlexTimeTotal,
        workTypes = workTypes,
    )
}

fun projectState(
    listIndex: Int = -1,
    date: String = "",
    projectName: String = "",
    projectTime: String = "00:00",
    kilometres: String = "",
    allowance: String = "",
    workType: String = "",
): SingleProjectState {
    return SingleProjectState(
        listIndex = listIndex,
        date = date,
        projectName = projectName,
        projectTime = projectTime,
        kilometres = kilometres,
        allowance = allowance,
        workType = workType,
    )
}

fun projectDetailsState(
    date: String = "",
    projectName: String = "",
    startTime: String = "00:00",
    endTime: String = "00:00",
    lunchStart: String = "00:00",
    lunchEnd: String = "00:00",
    breakStart: String = "00:00",
    breakEnd: String = "00:00",
    projectTime: String = "00:00",
    lunchTimeEstimate: String = "00:00",
): ProjectDetailsState {
    return ProjectDetailsState(
        date = date,
        projectName = projectName,
        startTime = startTime,
        endTime = endTime,
        lunchStart = lunchStart,
        lunchEnd = lunchEnd,
        breakStart = breakStart,
        breakEnd = breakEnd,
        projectTime = projectTime,
        lunchTimeEstimate = lunchTimeEstimate,
    )
}

fun workdayStatsRow(
    date: String,
    workTimeByDateEstimate: String,
): WorkdayStatsRow {
    return WorkdayStatsRow(
        date = date,
        workTimeByDateEstimate = workTimeByDateEstimate,
    )
}


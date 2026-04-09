package com.akiwiksten.worktime30.navigation

import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.CALENDAR_SCREEN
import com.akiwiksten.worktime30.core.INTRO_SCREEN
import com.akiwiksten.worktime30.core.PROJECTS_SCREEN
import com.akiwiksten.worktime30.core.SETTINGS_SCREEN
import com.akiwiksten.worktime30.core.SINGLE_PROJECT_SCREEN
import com.akiwiksten.worktime30.core.WORKDAY_SCREEN
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity

// Navigation routes
sealed class Screen(val route: String, val titleResId: Int? = null) {
    object Calendar : Screen(CALENDAR_SCREEN, R.string.calendar)
    object Projects : Screen(PROJECTS_SCREEN, R.string.projects)
    object Settings : Screen(SETTINGS_SCREEN, R.string.settings)
    data class Workday(
        val projectName: String? = null,
        val workday: WorkdayEntity? = null,
        val workStats: WorkStatsEntity? = null
    ) : Screen(WORKDAY_SCREEN, R.string.work_day)
    object Intro : Screen(INTRO_SCREEN)
    data class SingleProject(
        val index: Int = -1,
        val projectName: String? = null,
        val projectTime: String? = null,
        val kilometres: String? = null,
        val allowance: String? = null,
        val workType: String? = null,
        val workday: WorkdayEntity? = null,
        val workStats: WorkStatsEntity? = null
    ) : Screen(SINGLE_PROJECT_SCREEN)
}

package com.akiwiksten.worktime30.navigation

import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.CALENDAR_SCREEN
import com.akiwiksten.worktime30.core.EDIT_WORK_DAY_SCREEN
import com.akiwiksten.worktime30.core.INTRO_SCREEN
import com.akiwiksten.worktime30.core.PROJECTS_SCREEN
import com.akiwiksten.worktime30.core.SETTINGS_SCREEN
import com.akiwiksten.worktime30.core.SINGLE_PROJECT_SCREEN

// Navigation routes
sealed class Screen(val route: String, val titleResId: Int? = null) {
    object Calendar : Screen(CALENDAR_SCREEN, R.string.calendar)
    object Projects : Screen(PROJECTS_SCREEN, R.string.projects)
    object Settings : Screen(SETTINGS_SCREEN, R.string.settings)
    object EditWorkDay : Screen("$EDIT_WORK_DAY_SCREEN/{id}") {
        fun create(id: String) = "$EDIT_WORK_DAY_SCREEN/$id"
    }
    object Intro : Screen("$INTRO_SCREEN/{id}") {
        fun create(id: String) = "$INTRO_SCREEN/$id"
    }
    data class SingleProject(val index: Int = -1, val workTime: String? = null) : Screen(SINGLE_PROJECT_SCREEN)
}

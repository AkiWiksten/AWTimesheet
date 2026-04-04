package com.akiwiksten.worktime30.navigation

import com.akiwiksten.worktime30.core.CALENDAR_SCREEN
import com.akiwiksten.worktime30.core.EDIT_WORK_DAY_SCREEN
import com.akiwiksten.worktime30.core.INTRO_SCREEN
import com.akiwiksten.worktime30.core.PROJECTS_SCREEN
import com.akiwiksten.worktime30.core.SETTINGS_SCREEN

// Navigation routes
sealed class Screen(val route: String) {
    object Calendar : Screen(CALENDAR_SCREEN)
    object Projects : Screen(PROJECTS_SCREEN)
    object Settings : Screen(SETTINGS_SCREEN)
    object EditWorkDay : Screen("$EDIT_WORK_DAY_SCREEN/{id}") {
        fun create(id: String) = "$EDIT_WORK_DAY_SCREEN/$id"
    }
    object Intro : Screen("$INTRO_SCREEN/{id}") {
        fun create(id: String) = "$INTRO_SCREEN/$id"
    }
}

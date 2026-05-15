package com.akiwiksten.awtimesheet.navigation

import android.os.Parcelable
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.core.CALENDAR_SCREEN
import com.akiwiksten.awtimesheet.core.INTRO_SCREEN
import com.akiwiksten.awtimesheet.core.PROJECTS_SCREEN
import com.akiwiksten.awtimesheet.core.PROJECT_DETAILS_SCREEN
import com.akiwiksten.awtimesheet.core.SETTINGS_SCREEN
import com.akiwiksten.awtimesheet.core.SINGLE_PROJECT_SCREEN
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import kotlinx.parcelize.Parcelize

// Navigation routes
sealed interface Screen : Parcelable {
    val route: String
    val titleResId: Int?

    @Parcelize
    data object Calendar : Screen {
        override val route: String get() = CALENDAR_SCREEN
        override val titleResId: Int get() = R.string.calendar
    }

    @Parcelize
    data object Workday : Screen {
        override val route: String get() = PROJECTS_SCREEN
        override val titleResId: Int get() = R.string.workday
    }

    @Parcelize
    data object Settings : Screen {
        override val route: String get() = SETTINGS_SCREEN
        override val titleResId: Int get() = R.string.settings
    }

    @Parcelize
    data class ProjectDetails(
        val projectDetails: ProjectDetailsState = ProjectDetailsState()
    ) : Screen {
        override val route: String get() = PROJECT_DETAILS_SCREEN
        override val titleResId: Int get() = R.string.project_details
    }

    @Parcelize
    data object Intro : Screen {
        override val route: String get() = INTRO_SCREEN
        override val titleResId: Int? get() = null
    }

    @Parcelize
    data class SingleProject(
        val index: Int = -1,
        val date: String? = null,
        val projectName: String? = null,
        val projectTime: String? = null,
        val kilometres: String? = null,
        val allowance: String? = null,
        val workType: String? = null,
        val projectDetails: ProjectDetailsState? = null,
        val settingsEstimates: SettingsState? = null
    ) : Screen {
        override val route: String get() = SINGLE_PROJECT_SCREEN
        override val titleResId: Int? get() = null
    }
}

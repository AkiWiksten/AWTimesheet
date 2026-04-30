package com.akiwiksten.worktime30.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.feature.calendar.CalendarScreen
import com.akiwiksten.worktime30.feature.intro.IntroScreen
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsArgs
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsScreen
import com.akiwiksten.worktime30.feature.projects.single.SingleProjectNavigationActions
import com.akiwiksten.worktime30.feature.projects.single.SingleProjectScreen
import com.akiwiksten.worktime30.feature.projects.single.SingleProjectScreenArgs
import com.akiwiksten.worktime30.feature.settings.SettingsScreen
import com.akiwiksten.worktime30.feature.workday.WorkdayScreen

@Composable
fun WorkTime30App() {
    val backStack = remember { mutableStateListOf<Any>(Screen.Intro) }
    val isIntroRoute = backStack.lastOrNull() is Screen.Intro

    if (isIntroRoute) {
        WorkTimeNavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Scaffold(
            bottomBar = { WorkTimeNavigationBar(backStack = backStack) }
        ) { padding ->
            WorkTimeNavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(paddingValues = padding)
            )
        }
    }
}

@Composable
internal fun WorkTimeNavDisplay(
    backStack: SnapshotStateList<Any>,
    modifier: Modifier = Modifier
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.pop() },
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Screen.Intro> {
                IntroScreen(onItemClick = { backStack.add(element = Screen.Calendar) })
            }
            entry<Screen.Calendar> { CalendarScreen() }
            entry<Screen.Workday> {
                WorkdayScreen(
                    onNavigateToSingleProject = { index ->
                        backStack.add(element = Screen.SingleProject(index = index))
                    }
                )
            }
            entry<Screen.Settings> { SettingsScreen() }
            entry<Screen.ProjectDetails> { screen ->
                ProjectDetailsEntry(screen = screen, backStack = backStack)
            }
            entry<Screen.SingleProject> { screen ->
                SingleProjectEntry(screen = screen, backStack = backStack)
            }
        }
    )
}

@Composable
private fun ProjectDetailsEntry(screen: Screen.ProjectDetails, backStack: SnapshotStateList<Any>) {
    ProjectDetailsScreen(
        args = ProjectDetailsArgs(
            projectName = screen.projectName,
            projectDetails = screen.projectDetails,
            settings = screen.settingsEstimates
        ),
        onNavigateBack = { backStack.pop() },
        onConfirm = { projectDetails, settings ->
            backStack.updateSingleProjectWorkTime(projectDetails = projectDetails, settings = settings)
        }
    )
}

@Composable
private fun SingleProjectEntry(screen: Screen.SingleProject, backStack: SnapshotStateList<Any>) {
    val initialSingleProjectState = SingleProjectState(
        index = screen.index,
        projectName = screen.projectName ?: "",
        projectTime = screen.projectTime ?: ZERO_TIME,
        kilometres = screen.kilometres ?: "",
        allowance = screen.allowance
            .takeUnless { it.isNullOrBlank() }
            ?: stringResource(id = R.string.no_allowance),
        workType = screen.workType ?: "",
    )
    SingleProjectScreen(
        args = SingleProjectScreenArgs(
            initialSingleProjectState = initialSingleProjectState,
            initialProjectDetails = screen.projectDetails,
            initialSettings = screen.settingsEstimates
        ),
        navigationActions = SingleProjectNavigationActions(
            onNavigateBack = { backStack.pop() },
            onOpenProjectDetails = { state, projectDetails, settings ->
                backStack.updateSingleProjectState(
                    state = state,
                    projectDetails = projectDetails,
                    settings = settings
                )
                backStack.add(
                    element = Screen.ProjectDetails(
                        projectName = state.projectName,
                        projectDetails = projectDetails,
                        settingsEstimates = settings
                    )
                )
            }
        )
    )
}

internal fun SnapshotStateList<Any>.pop() {
    if (isNotEmpty()) {
        removeAt(index = size - 1)
    }
}

internal fun SnapshotStateList<Any>.updateSingleProjectWorkTime(
    projectDetails: ProjectDetailsState,
    settings: SettingsState
) {
    pop()
    val currentLast = lastOrNull()
    if (currentLast is Screen.SingleProject) {
        this[size - 1] = currentLast.copy(
            projectTime = projectDetails.projectTime,
            projectDetails = projectDetails,
            settingsEstimates = settings
        )
    }
}

internal fun SnapshotStateList<Any>.updateSingleProjectState(
    state: SingleProjectState,
    projectDetails: ProjectDetailsState?,
    settings: SettingsState?
) {
    val index = size - 1
    val current = getOrNull(index = index)
    if (current is Screen.SingleProject) {
        this[index] = current.copy(
            projectName = state.projectName,
            projectTime = state.projectTime,
            kilometres = state.kilometres,
            allowance = state.allowance,
            workType = state.workType,
            projectDetails = projectDetails,
            settingsEstimates = settings
        )
    }
}

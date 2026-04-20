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
import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.R
import com.akiwiksten.worktime30.feature.calendar.CalendarScreen
import com.akiwiksten.worktime30.feature.intro.IntroScreen
import com.akiwiksten.worktime30.feature.projects.daily.ProjectsScreen
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState
import com.akiwiksten.worktime30.feature.projects.single.SingleProjectScreen
import com.akiwiksten.worktime30.feature.projects.single.details.ProjectDetailsArgs
import com.akiwiksten.worktime30.feature.projects.single.details.ProjectDetailsScreen
import com.akiwiksten.worktime30.feature.projects.single.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.single.details.WorkStatsState
import com.akiwiksten.worktime30.feature.settings.SettingsScreen

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
            entry<Screen.Projects> {
                ProjectsScreen(
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
            workStats = screen.workStats
        ),
        onNavigateBack = { backStack.pop() },
        onConfirm = { projectDetails, workStats ->
            backStack.updateSingleProjectWorkTime(projectDetails = projectDetails, workStats = workStats)
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
        allowance = screen.allowance ?: stringResource(id = R.string.no_allowance),
        workType = screen.workType ?: "",
        projectDetails = screen.projectDetails,
        workStats = screen.workStats,
    )
    SingleProjectScreen(
        initialSingleProjectState = initialSingleProjectState,
        onNavigateBack = { backStack.pop() },
        onOpenProjectDetails = { state ->
            backStack.updateSingleProjectState(state = state)
            backStack.add(
                element = Screen.ProjectDetails(
                    projectName = state.projectName,
                    projectDetails = state.projectDetails,
                    workStats = state.workStats
                )
            )
        }
    )
}

internal fun SnapshotStateList<Any>.pop() {
    if (isNotEmpty()) {
        removeAt(index = size - 1)
    }
}

internal fun SnapshotStateList<Any>.updateSingleProjectWorkTime(
    projectDetails: ProjectDetailsState,
    workStats: WorkStatsState
) {
    pop()
    val currentLast = lastOrNull()
    if (currentLast is Screen.SingleProject) {
        this[size - 1] = currentLast.copy(
            projectTime = projectDetails.projectTime,
            projectDetails = projectDetails,
            workStats = workStats
        )
    }
}

internal fun SnapshotStateList<Any>.updateSingleProjectState(state: SingleProjectState) {
    val index = size - 1
    val current = getOrNull(index = index)
    if (current is Screen.SingleProject) {
        this[index] = current.copy(
            projectName = state.projectName,
            projectTime = state.projectTime,
            kilometres = state.kilometres,
            allowance = state.allowance,
            workType = state.workType,
            projectDetails = state.projectDetails,
            workStats = state.workStats
        )
    }
}

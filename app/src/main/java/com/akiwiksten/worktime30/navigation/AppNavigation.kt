package com.akiwiksten.worktime30.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.feature.calendar.CalendarScreen
import com.akiwiksten.worktime30.feature.intro.IntroScreen
import com.akiwiksten.worktime30.feature.projects.ProjectsScreen
import com.akiwiksten.worktime30.feature.projects.SingleProjectScreen
import com.akiwiksten.worktime30.feature.projects.SingleProjectState
import com.akiwiksten.worktime30.feature.settings.SettingsScreen
import com.akiwiksten.worktime30.feature.workday.WorkdayArgs
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
            entry<Screen.Projects> {
                ProjectsScreen(
                    onNavigateToSingleProject = { index ->
                        backStack.add(element = Screen.SingleProject(index = index))
                    }
                )
            }
            entry<Screen.Settings> { SettingsScreen() }
            entry<Screen.Workday> { screen ->
                WorkdayEntry(screen = screen, backStack = backStack)
            }
            entry<Screen.SingleProject> { screen ->
                SingleProjectEntry(screen = screen, backStack = backStack)
            }
        }
    )
}

@Composable
private fun WorkdayEntry(screen: Screen.Workday, backStack: SnapshotStateList<Any>) {
    WorkdayScreen(
        args = WorkdayArgs(
            projectName = screen.projectName,
            workday = screen.workday,
            workStats = screen.workStats
        ),
        onNavigateBack = { backStack.pop() },
        onConfirm = { workday, workStats ->
            backStack.updateSingleProjectWorkTime(workday = workday, workStats = workStats)
        }
    )
}

@Composable
private fun SingleProjectEntry(screen: Screen.SingleProject, backStack: SnapshotStateList<Any>) {
    SingleProjectScreen(
        index = screen.index,
        onNavigateBack = { backStack.pop() },
        onOpenWorkday = { state ->
            backStack.updateSingleProjectState(state = state)
            backStack.add(
                element = Screen.Workday(
                    projectName = state.projectName,
                    workday = state.workday,
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
    workday: WorkdayEntity,
    workStats: WorkStatsEntity
) {
    pop()
    val currentLast = lastOrNull()
    if (currentLast is Screen.SingleProject) {
        this[size - 1] = currentLast.copy(
            projectTime = workday.projectTime,
            workday = workday,
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
            workday = state.workday,
            workStats = state.workStats
        )
    }
}

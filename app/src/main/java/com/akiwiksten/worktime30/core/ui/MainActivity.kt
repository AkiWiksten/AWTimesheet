package com.akiwiksten.worktime30.core.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.feature.calendar.CalendarScreen
import com.akiwiksten.worktime30.feature.intro.IntroScreen
import com.akiwiksten.worktime30.feature.projects.ProjectDialogState
import com.akiwiksten.worktime30.feature.projects.ProjectsScreen
import com.akiwiksten.worktime30.feature.projects.SingleProjectArgs
import com.akiwiksten.worktime30.feature.projects.SingleProjectScreen
import com.akiwiksten.worktime30.feature.settings.SettingsScreen
import com.akiwiksten.worktime30.feature.workday.WorkdayArgs
import com.akiwiksten.worktime30.feature.workday.WorkdayScreen
import com.akiwiksten.worktime30.navigation.Screen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WorkTime30Theme {
                WorkTime30App()
            }
        }
    }
}

// App composable with Navigation 3
@Composable
fun WorkTime30App() {
    val backStack = remember { mutableStateListOf<Any>(Screen.Intro) }

    Scaffold(
        bottomBar = { WorkTimeNavigationBar(backStack = backStack) }
    ) { padding ->
        WorkTimeNavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(paddingValues = padding)
        )
    }
}

@Composable
private fun WorkTimeNavigationBar(backStack: SnapshotStateList<Any>) {
    val navigationScreens = listOf(Screen.Calendar, Screen.Projects, Screen.Settings)
    NavigationBar {
        navigationScreens.forEach { screen ->
            val isSelected = backStack.lastOrNull() == screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { if (!isSelected) backStack.add(element = screen) },
                icon = { ScreenIcon(screen = screen) },
                label = { ScreenLabel(screen = screen) }
            )
        }
    }
}

@Composable
private fun ScreenIcon(screen: Screen) {
    val icon = when (screen) {
        Screen.Calendar -> Icons.Default.CalendarMonth
        Screen.Projects -> Icons.AutoMirrored.Filled.List
        Screen.Settings -> Icons.Default.Settings
        else -> Icons.Default.Home
    }
    Icon(imageVector = icon, contentDescription = null)
}

@Composable
private fun ScreenLabel(screen: Screen) {
    val label = screen.titleResId?.let { stringResource(id = it) } ?: screen.route
    Text(text = label)
}

@Composable
private fun WorkTimeNavDisplay(
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
        args = SingleProjectArgs(
            index = screen.index,
            projectName = screen.projectName,
            workTime = screen.projectTime,
            kilometres = screen.kilometres,
            allowance = screen.allowance,
            workType = screen.workType,
            workday = screen.workday,
            workStats = screen.workStats
        ),
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

private fun SnapshotStateList<Any>.pop() {
    if (isNotEmpty()) {
        removeAt(index = size - 1)
    }
}

private fun SnapshotStateList<Any>.updateSingleProjectWorkTime(
    workday: WorkdayEntity,
    workStats: WorkStatsEntity
) {
    pop()
    val currentLast = lastOrNull()
    if (currentLast is Screen.SingleProject) {
        this[size - 1] = currentLast.copy(
            projectTime = workday.workTimeToday,
            workday = workday,
            workStats = workStats
        )
    }
}

private fun SnapshotStateList<Any>.updateSingleProjectState(state: ProjectDialogState) {
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

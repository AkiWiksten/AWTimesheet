package com.akiwiksten.worktime30.core

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
import com.akiwiksten.worktime30.feature.calendar.CalendarScreen
import com.akiwiksten.worktime30.feature.editworkday.EditWorkDayScreen
import com.akiwiksten.worktime30.feature.intro.IntroScreen
import com.akiwiksten.worktime30.feature.projects.ProjectsScreen
import com.akiwiksten.worktime30.feature.projects.SingleProjectScreen
import com.akiwiksten.worktime30.feature.settings.SettingsScreen
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
        bottomBar = { WorkTimeNavigationBar(backStack) }
    ) { padding ->
        WorkTimeNavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(padding)
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
                onClick = { if (!isSelected) backStack.add(screen) },
                icon = { ScreenIcon(screen) },
                label = { ScreenLabel(screen) }
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
    val label = screen.titleResId?.let { stringResource(it) } ?: screen.route
    Text(label)
}

@Composable
private fun WorkTimeNavDisplay(
    backStack: SnapshotStateList<Any>,
    modifier: Modifier = Modifier
) {
    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Screen.Intro> {
                IntroScreen(onItemClick = { backStack.add(Screen.Calendar) })
            }
            entry<Screen.Calendar> { CalendarScreen() }
            entry<Screen.Projects> {
                ProjectsScreen(
                    onNavigateToSingleProject = { index -> backStack.add(Screen.SingleProject(index)) }
                )
            }
            entry<Screen.Settings> { SettingsScreen() }
            entry<Screen.EditWorkDay> {
                EditWorkDayScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onSave = { workTime ->
                        backStack.removeLastOrNull()
                        val last = backStack.lastOrNull()
                        if (last is Screen.SingleProject) {
                            backStack[backStack.size - 1] = last.copy(workTime = workTime)
                        }
                    }
                )
            }
            entry<Screen.SingleProject> { screen ->
                SingleProjectScreen(
                    index = screen.index,
                    workTime = screen.workTime,
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onOpenEditWorkDay = { backStack.add(Screen.EditWorkDay) }
                )
            }
        }
    )
}

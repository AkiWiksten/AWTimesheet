package com.akiwiksten.worktime30.core

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.akiwiksten.worktime30.core.theme.WorkTime30Theme
import com.akiwiksten.worktime30.feature.calendar.CalendarScreen
import com.akiwiksten.worktime30.feature.editworkday.EditWorkDayScreen
import com.akiwiksten.worktime30.feature.intro.IntroScreen
import com.akiwiksten.worktime30.feature.projects.ProjectsScreen
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
@Suppress("LongMethod")
@Composable
fun WorkTime30App() {
    val backStack = remember { mutableStateListOf<Any>(Screen.Intro) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(Screen.Calendar, Screen.Projects, Screen.Settings).forEach { screen ->
                    NavigationBarItem(
                        selected = backStack.lastOrNull() == screen,
                        onClick = {
                            if (backStack.lastOrNull() != screen) {
                                backStack.add(screen)
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(screen.route) }
                    )
                }
            }
        }
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            modifier = Modifier.padding(padding),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            entryProvider = entryProvider {
                entry<Screen.Intro> {
                    IntroScreen(
                        onItemClick = { backStack.add(Screen.Calendar) }
                    )
                }
                entry<Screen.Calendar> {
                    CalendarScreen()
                }
                entry<Screen.Projects> {
                    ProjectsScreen()
                }
                entry<Screen.Settings> {
                    SettingsScreen()
                }
                entry<Screen.EditWorkDay> {
                    EditWorkDayScreen(
                        onItemClick = { backStack.add(Screen.Projects) }
                    )
                }
            }
        )
    }
}

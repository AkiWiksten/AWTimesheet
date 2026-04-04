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
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

// App composable with bottom navigation
@Suppress("FunctionNaming")
@Composable
fun WorkTime30App() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(Screen.Calendar, Screen.Projects, Screen.Settings).forEach { screen ->
                    NavigationBarItem(
                        selected = false,
                        onClick = { navController.navigate(screen.route) },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text(screen.route) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController, startDestination = Screen.Intro.route, modifier = Modifier.padding(padding)) {

            composable(Screen.Calendar.route) {
                CalendarScreen()
            }

            composable(Screen.Projects.route) {
                ProjectsScreen()
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(Screen.EditWorkDay.route) {
                EditWorkDayScreen(
                    onItemClick = { navController.navigate(Screen.Projects.route) },
                )
            }

            composable(Screen.Intro.route) {
                IntroScreen(
                    onItemClick = { navController.navigate(Screen.Calendar.route) },
                )
            }
        }
    }
}

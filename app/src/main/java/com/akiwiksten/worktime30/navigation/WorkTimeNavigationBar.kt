package com.akiwiksten.worktime30.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun WorkTimeNavigationBar(backStack: SnapshotStateList<Any>) {
    val navigationScreens = listOf(Screen.Calendar, Screen.Workday, Screen.Settings)
    Surface(shadowElevation = 8.dp, tonalElevation = 8.dp) {
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
}

@Composable
private fun ScreenIcon(screen: Screen) {
    val icon = when (screen) {
        Screen.Calendar -> Icons.Default.CalendarMonth
        Screen.Workday -> Icons.AutoMirrored.Filled.List
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

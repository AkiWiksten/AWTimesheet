package com.akiwiksten.awtimesheet.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.res.stringResource
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.core.ui.UnsavedChangesDialog

@Composable
internal fun AWTimesheetNavigationBar(
    backStack: SnapshotStateList<Any>,
    settingsHasUnsavedChanges: Boolean,
    onSaveSettingsChanges: () -> Unit,
    onDiscardSettingsChanges: () -> Unit
) {
    val navigationScreens = listOf(Screen.Calendar, Screen.Workday, Screen.Settings)
    var pendingScreen by remember { mutableStateOf<Screen?>(value = null) }
    val isShowingUnsavedDialog = pendingScreen != null
    val unsavedMessage = stringResource(id = R.string.unsaved_data_message)

    if (isShowingUnsavedDialog) {
        UnsavedChangesDialog(
            onDismiss = { pendingScreen = null },
            onDiscard = {
                onDiscardSettingsChanges()
                pendingScreen?.let { backStack.add(element = it) }
                pendingScreen = null
            },
            onSave = {
                onSaveSettingsChanges()
                pendingScreen?.let { backStack.add(element = it) }
                pendingScreen = null
            },
            dialogText = unsavedMessage
        )
    }

    NavigationBar {
        navigationScreens.forEach { screen ->
                val isSelected = backStack.lastOrNull() == screen
                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) return@NavigationBarItem

                        val isLeavingSettings = backStack.lastOrNull() == Screen.Settings
                        if (isLeavingSettings && settingsHasUnsavedChanges) {
                            pendingScreen = screen
                        } else {
                            backStack.add(element = screen)
                        }
                    },
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

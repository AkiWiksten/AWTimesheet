package com.akiwiksten.awtimesheet.navigation

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.core.ui.LocalContentBottomPadding
import com.akiwiksten.awtimesheet.core.ui.UnsavedChangesDialog
import com.akiwiksten.awtimesheet.feature.absence.AbsenceScreen
import com.akiwiksten.awtimesheet.feature.absence.AbsenceViewModel
import com.akiwiksten.awtimesheet.feature.absence.CreateAbsenceScreen
import com.akiwiksten.awtimesheet.feature.calendar.CalendarScreen
import com.akiwiksten.awtimesheet.feature.intro.IntroScreen
import com.akiwiksten.awtimesheet.feature.settings.SettingsScreen
import com.akiwiksten.awtimesheet.feature.workday.WorkdayScreen

internal data class SettingsNavigationGuard(
    val hasUnsavedChanges: Boolean,
    val onSaveChanges: () -> Unit,
    val onDiscardChanges: () -> Unit,
    val onUnsavedChangesChanged: (Boolean) -> Unit,
    val registerUnsavedActions: ((() -> Unit)?, (() -> Unit)?) -> Unit
)

@Composable
internal fun rememberSettingsNavigationGuard(): SettingsNavigationGuard {
    var settingsHasUnsavedChanges by remember { mutableStateOf(value = false) }
    var settingsSaveChanges by remember { mutableStateOf<(() -> Unit)?>(value = null) }
    var settingsDiscardChanges by remember { mutableStateOf<(() -> Unit)?>(value = null) }

    return SettingsNavigationGuard(
        hasUnsavedChanges = settingsHasUnsavedChanges,
        onSaveChanges = {
            settingsSaveChanges?.invoke()
            settingsHasUnsavedChanges = false
        },
        onDiscardChanges = {
            settingsDiscardChanges?.invoke()
            settingsHasUnsavedChanges = false
        },
        onUnsavedChangesChanged = { settingsHasUnsavedChanges = it },
        registerUnsavedActions = { onSave, onDiscard ->
            settingsSaveChanges = onSave
            settingsDiscardChanges = onDiscard
        }
    )
}

@Composable
internal fun MainAppScaffold(
    backStack: SnapshotStateList<Any>,
    portraitWidth: Dp,
    settingsNavigationGuard: SettingsNavigationGuard
) {
    Scaffold(
        bottomBar = {
            PortraitWidthContainer(portraitWidth = portraitWidth) {
                AWTimesheetNavigationBar(
                    backStack = backStack,
                    settingsHasUnsavedChanges = settingsNavigationGuard.hasUnsavedChanges,
                    onSaveSettingsChanges = settingsNavigationGuard.onSaveChanges,
                    onDiscardSettingsChanges = settingsNavigationGuard.onDiscardChanges
                )
            }
        }
    ) { innerPadding ->
        CompositionLocalProvider(LocalContentBottomPadding provides innerPadding.calculateBottomPadding()) {
            PortraitWidthContainer(
                portraitWidth = portraitWidth,
                modifier = Modifier
                    .fillMaxSize()
                    .consumeWindowInsets(innerPadding)
            ) {
                WorkTimeNavDisplay(
                    backStack = backStack,
                    settingsNavigationGuard = settingsNavigationGuard,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
internal fun WorkTimeNavDisplay(
    backStack: SnapshotStateList<Any>,
    settingsNavigationGuard: SettingsNavigationGuard,
    modifier: Modifier = Modifier
) {
    var showUnsavedBackDialog by remember { mutableStateOf(value = false) }
    val absenceViewModel: AbsenceViewModel = hiltViewModel()

    SettingsBackNavigationDialog(
        isVisible = showUnsavedBackDialog,
        onDismiss = { showUnsavedBackDialog = false },
        onSave = {
            settingsNavigationGuard.onSaveChanges()
            showUnsavedBackDialog = false
            backStack.pop()
        },
        onDiscard = {
            settingsNavigationGuard.onDiscardChanges()
            showUnsavedBackDialog = false
            backStack.pop()
        }
    )

    MainNavDisplayContent(
        backStack = backStack,
        settingsNavigationGuard = settingsNavigationGuard,
        absenceViewModel = absenceViewModel,
        onUnsavedChangesBlocked = { showUnsavedBackDialog = true },
        modifier = modifier
    )
}

@Composable
private fun MainNavDisplayContent(
    backStack: SnapshotStateList<Any>,
    settingsNavigationGuard: SettingsNavigationGuard,
    absenceViewModel: AbsenceViewModel,
    onUnsavedChangesBlocked: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavDisplay(
        backStack = backStack,
        onBack = createGuardedBackAction(
            backStack = backStack,
            hasUnsavedChanges = settingsNavigationGuard.hasUnsavedChanges,
            onUnsavedChangesBlocked = onUnsavedChangesBlocked
        ),
        modifier = modifier,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = appEntryProvider(
            backStack = backStack,
            settingsNavigationGuard = settingsNavigationGuard,
            absenceViewModel = absenceViewModel
        )
    )
}

private fun appEntryProvider(
    backStack: SnapshotStateList<Any>,
    settingsNavigationGuard: SettingsNavigationGuard,
    absenceViewModel: AbsenceViewModel
) = entryProvider<Any> {
    entry<Screen.Intro> {
        IntroNavEntry(backStack = backStack)
    }
    entry<Screen.Calendar> {
        CalendarScreen(onNavigateToAbsence = { backStack.add(element = Screen.Absence) })
    }
    entry<Screen.Absence> {
        AbsenceScreen(
            onNavigateBack = { backStack.pop() },
            onNavigateToCreateAbsence = { backStack.add(element = Screen.CreateAbsence) },
            viewModel = absenceViewModel
        )
    }
    entry<Screen.CreateAbsence> {
        CreateAbsenceScreen(
            onNavigateBack = { backStack.pop() },
            onAbsenceCreated = absenceViewModel::addAbsence
        )
    }
    entry<Screen.Workday> {
        WorkdayNavEntry(backStack = backStack)
    }
    entry<Screen.Settings> {
        SettingsNavEntry(settingsNavigationGuard = settingsNavigationGuard)
    }
    entry<Screen.ProjectDetails> { screen ->
        ProjectDetailsEntry(screen = screen, backStack = backStack)
    }
    entry<Screen.SingleProject> { screen ->
        SingleProjectEntry(screen = screen, backStack = backStack)
    }
}

@Composable
private fun IntroNavEntry(backStack: SnapshotStateList<Any>) {
    IntroScreen(onItemClick = { backStack.add(element = Screen.Calendar) })
}

@Composable
private fun WorkdayNavEntry(backStack: SnapshotStateList<Any>) {
    WorkdayScreen(
        onNavigateToSingleProject = { project ->
            backStack.add(
                element = Screen.SingleProject(
                    listIndex = project.listIndex,
                    projectName = project.projectName
                )
            )
        }
    )
}

@Composable
private fun SettingsNavEntry(settingsNavigationGuard: SettingsNavigationGuard) {
    SettingsScreen(
        onUnsavedChangesChanged = settingsNavigationGuard.onUnsavedChangesChanged,
        registerUnsavedActions = settingsNavigationGuard.registerUnsavedActions
    )
}

internal fun createGuardedBackAction(
    backStack: SnapshotStateList<Any>,
    hasUnsavedChanges: Boolean,
    onUnsavedChangesBlocked: () -> Unit
): () -> Unit {
    return {
        val isLeavingSettings = backStack.lastOrNull() == Screen.Settings
        if (isLeavingSettings && hasUnsavedChanges) {
            onUnsavedChangesBlocked()
        } else {
            backStack.pop()
        }
    }
}

@Composable
private fun SettingsBackNavigationDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    if (!isVisible) return

    UnsavedChangesDialog(
        onDismiss = onDismiss,
        onDiscard = onDiscard,
        onSave = onSave,
        dialogText = stringResource(id = R.string.unsaved_data_message)
    )
}

package com.akiwiksten.awtimesheet.navigation

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.akiwiksten.awtimesheet.core.ui.LocalContentBottomPadding
import com.akiwiksten.awtimesheet.core.ui.UnsavedChangesDialog
import kotlinx.parcelize.Parcelize
import kotlin.math.min
import com.akiwiksten.awtimesheet.core.R as CoreR

@Parcelize
private class BackStackData(val items: ArrayList<Screen>) : Parcelable

private val BackStackSaver: Saver<SnapshotStateList<Any>, BackStackData> = Saver(
    save = { stack -> BackStackData(ArrayList(stack.filterIsInstance<Screen>())) },
    restore = { data ->
        mutableStateListOf<Any>().apply {
            addAll(data.items)
        }
    }
)

@Composable
fun AWTimesheetApp() {
    val backStack = rememberSaveable(saver = BackStackSaver) { mutableStateListOf<Any>(Screen.Intro) }
    val settingsNavigationGuard = rememberSettingsNavigationGuard()
    val isIntroRoute = backStack.lastOrNull() is Screen.Intro
    val portraitWidth = currentPortraitWidthDp()

    if (isIntroRoute) {
        WorkTimeNavDisplay(
            backStack = backStack,
            settingsNavigationGuard = settingsNavigationGuard,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        MainAppScaffold(
            backStack = backStack,
            portraitWidth = portraitWidth,
            settingsNavigationGuard = settingsNavigationGuard
        )
    }
}

@Composable
private fun currentPortraitWidthDp(): Dp {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) {
        min(windowInfo.containerSize.width, windowInfo.containerSize.height).toDp()
    }
}

@Composable
internal fun PortraitWidthContainer(
    portraitWidth: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .width(width = portraitWidth)
        ) {
            content()
        }
    }
}

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
                    .statusBarsPadding()
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
        onUnsavedChangesBlocked = { showUnsavedBackDialog = true },
        modifier = modifier
    )
}

@Composable
private fun MainNavDisplayContent(
    backStack: SnapshotStateList<Any>,
    settingsNavigationGuard: SettingsNavigationGuard,
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
            settingsNavigationGuard = settingsNavigationGuard
        )
    )
}

private fun appEntryProvider(
    backStack: SnapshotStateList<Any>,
    settingsNavigationGuard: SettingsNavigationGuard
) = entryProvider<Any> {
    entry<Screen.Intro> {
        IntroNavEntry(backStack = backStack)
    }
    entry<Screen.Calendar> {
        CalendarNavEntry()
    }
    entry<Screen.Absence> {
        AbsenceNavEntry(backStack = backStack)
    }
    entry<Screen.CreateAbsence> {
        CreateAbsenceNavEntry(backStack = backStack)
    }
    entry<Screen.Workday> {
        WorkdayNavEntry(backStack = backStack)
    }
    entry<Screen.Settings> {
        SettingsNavEntry(settingsNavigationGuard = settingsNavigationGuard)
    }
    entry<Screen.ProjectDetails> { screen ->
        ProjectDetailsNavEntry(screen = screen, backStack = backStack)
    }
    entry<Screen.SingleProject> { screen ->
        SingleProjectNavEntry(screen = screen, backStack = backStack)
    }
    entry<Screen.DistanceCalculator> { screen ->
        DistanceCalculatorNavEntry(screen = screen, backStack = backStack)
    }
    entry<Screen.LocationPicker> { screen ->
        LocationPickerNavEntry(screen = screen, backStack = backStack)
    }
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
        dialogText = stringResource(id = CoreR.string.unsaved_data_message)
    )
}

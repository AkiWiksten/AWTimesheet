package com.akiwiksten.awtimesheet.navigation

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.projectdetails.ProjectDetailsScreen
import com.akiwiksten.awtimesheet.feature.singleproject.SingleProjectScreen
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectNavigationActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectScreenArgs
import kotlinx.parcelize.Parcelize
import kotlin.math.min

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
    val portraitWidth = rememberPortraitWidthDp()

    if (isIntroRoute) {
        AppNavHost(
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
private fun rememberPortraitWidthDp(): Dp {
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

@Composable
internal fun ProjectDetailsEntry(screen: Screen.ProjectDetails, backStack: SnapshotStateList<Any>) {
    ProjectDetailsScreen(
        projectName = screen.projectName ?: "",
        projectTime = screen.projectTime ?: "",
        onNavigateBack = { backStack.pop() },
        onConfirm = { projectName, projectTime ->
            backStack.updateSingleProjectWorkTime(
                projectName = projectName,
                projectTime = projectTime
            )
        }
    )
}

@Composable
internal fun SingleProjectEntry(screen: Screen.SingleProject, backStack: SnapshotStateList<Any>) {
    SingleProjectScreen(
        navigationActions = SingleProjectNavigationActions(
            onNavigateBack = { backStack.pop() },
            onOpenProjectDetails = { singleProject ->
                backStack.updateSingleProjectState(
                    singleProject = singleProject,
                )
                backStack.add(
                    element = Screen.ProjectDetails(
                        projectTime = singleProject.projectTime ?: ZERO_TIME,
                        projectName = singleProject.projectName
                    )
                )
            }
        ),
        projectName = screen.projectName ?: "",
        isAddMode = screen.listIndex == -1,
        listIndex = screen.listIndex,
    )
}

internal fun SnapshotStateList<Any>.pop() {
    if (isNotEmpty()) {
        removeAt(index = size - 1)
    }
}

internal fun SnapshotStateList<Any>.updateSingleProjectWorkTime(
    projectName: String,
    projectTime: String,
) {
    pop()
    val currentLast = lastOrNull()
    if (currentLast is Screen.SingleProject) {
        this[size - 1] = currentLast.copy(
            projectName = projectName,
            projectTime = projectTime
        )
    }
}

internal fun SnapshotStateList<Any>.updateSingleProjectState(
    singleProject: SingleProjectState,
) {
    val index = size - 1
    val current = getOrNull(index = index)
    if (current is Screen.SingleProject) {
        this[index] = current.copy(
            projectName = singleProject.projectName,
        )
    }
}

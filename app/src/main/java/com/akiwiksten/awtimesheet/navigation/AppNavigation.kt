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
import androidx.compose.ui.unit.Dp
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.projectdetails.ProjectDetailsScreen
import com.akiwiksten.awtimesheet.feature.singleproject.SingleProjectScreen
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectNavigationActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectRouteArgs
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
        onConfirm = { details ->
            backStack.updateSingleProjectWorkTime(
                details = Screen.ProjectDetails(
                    projectName = details.projectName,
                    projectTime = details.projectTime,
                    startTime = details.startTime,
                    endTime = details.endTime,
                    lunchStart = details.lunchStart,
                    lunchEnd = details.lunchEnd,
                    breakStart = details.breakStart,
                    breakEnd = details.breakEnd,
                )
            )
        }
    )
}

@Composable
internal fun SingleProjectEntry(screen: Screen.SingleProject, backStack: SnapshotStateList<Any>) {
    SingleProjectScreen(
        routeArgs = SingleProjectRouteArgs(
            projectName = screen.projectName ?: "",
            projectTime = screen.projectTime ?: "",
            isAddMode = screen.listIndex == -1,
            listIndex = screen.listIndex,
            projectDetails = ProjectDetailsState(
                projectName = screen.details?.projectName ?: "",
                projectTime = screen.details?.projectTime ?: "",
                startTime = screen.details?.startTime ?: "",
                endTime = screen.details?.endTime ?: "",
                lunchStart = screen.details?.lunchStart ?: "",
                lunchEnd = screen.details?.lunchEnd ?: "",
                breakStart = screen.details?.breakStart ?: "",
                breakEnd = screen.details?.breakEnd ?: ""
            )
        ),
        navigationActions = SingleProjectNavigationActions(
            onNavigateBack = { backStack.pop() },
            onOpenProjectDetails = { singleProject ->
                backStack.add(
                    element = Screen.ProjectDetails(
                        projectTime = singleProject.projectTime,
                        projectName = singleProject.projectName
                    )
                )
            }
        )
    )
}

internal fun SnapshotStateList<Any>.pop() {
    if (isNotEmpty()) {
        removeAt(index = size - 1)
    }
}

internal fun SnapshotStateList<Any>.updateSingleProjectWorkTime(
    details: Screen.ProjectDetails
) {
    pop()
    val currentLast = lastOrNull()
    if (currentLast is Screen.SingleProject) {
        this[size - 1] = currentLast.copy(
            details = details
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
            projectTime = singleProject.projectTime,
            kilometres = singleProject.kilometres,
            allowance = singleProject.allowance,
            workType = singleProject.workType
        )
    }
}

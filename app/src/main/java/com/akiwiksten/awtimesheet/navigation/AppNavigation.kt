package com.akiwiksten.awtimesheet.navigation

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.akiwiksten.awtimesheet.R
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.calendar.CalendarScreen
import com.akiwiksten.awtimesheet.feature.intro.IntroScreen
import com.akiwiksten.awtimesheet.feature.projects.details.ProjectDetailsScreen
import com.akiwiksten.awtimesheet.feature.projects.single.SingleProjectNavigationActions
import com.akiwiksten.awtimesheet.feature.projects.single.SingleProjectScreen
import com.akiwiksten.awtimesheet.feature.projects.single.SingleProjectScreenArgs
import com.akiwiksten.awtimesheet.feature.settings.SettingsScreen
import com.akiwiksten.awtimesheet.feature.workday.WorkdayScreen
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
    val isIntroRoute = backStack.lastOrNull() is Screen.Intro
    val portraitWidth = rememberPortraitWidthDp()

    if (isIntroRoute) {
        WorkTimeNavDisplay(
            backStack = backStack,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Scaffold(
            bottomBar = {
                PortraitWidthContainer(portraitWidth = portraitWidth) {
                    WorkTimeNavigationBar(backStack = backStack)
                }
            }
        ) { padding ->
            PortraitWidthContainer(
                portraitWidth = portraitWidth,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = padding)
            ) {
                WorkTimeNavDisplay(
                    backStack = backStack,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
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
internal fun WorkTimeNavDisplay(
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
            entry<Screen.Workday> {
                WorkdayScreen(
                    onNavigateToSingleProject = { project ->
                        backStack.add(
                            element = Screen.SingleProject(
                                index = project.index,
                                date = project.date,
                                projectName = project.projectName,
                                projectTime = project.projectTime,
                                kilometres = project.kilometres,
                                allowance = project.allowance,
                                workType = project.workType
                            )
                        )
                    }
                )
            }
            entry<Screen.Settings> { SettingsScreen() }
            entry<Screen.ProjectDetails> { screen ->
                ProjectDetailsEntry(screen = screen, backStack = backStack)
            }
            entry<Screen.SingleProject> { screen ->
                SingleProjectEntry(screen = screen, backStack = backStack)
            }
        }
    )
}

@Composable
private fun ProjectDetailsEntry(screen: Screen.ProjectDetails, backStack: SnapshotStateList<Any>) {
    ProjectDetailsScreen(
        projectDetails = screen.projectDetails,
        onNavigateBack = { backStack.pop() },
        onConfirm = { projectDetails, settings ->
            backStack.updateSingleProjectWorkTime(projectDetails = projectDetails, settings = settings)
        }
    )
}

@Composable
private fun SingleProjectEntry(screen: Screen.SingleProject, backStack: SnapshotStateList<Any>) {
    val initialSingleProjectState = SingleProjectState(
        index = screen.index,
        projectName = screen.projectName ?: "",
        projectTime = screen.projectTime ?: ZERO_TIME,
        kilometres = screen.kilometres ?: "",
        allowance = screen.allowance
            .takeUnless { it.isNullOrBlank() }
            ?: stringResource(id = R.string.no_allowance),
        workType = screen.workType ?: "",
        date = screen.date ?: ""
    )
    SingleProjectScreen(
        args = SingleProjectScreenArgs(
            initialSingleProjectState = initialSingleProjectState,
            initialProjectDetails = screen.projectDetails,
            initialSettings = screen.settingsEstimates
        ),
        navigationActions = SingleProjectNavigationActions(
            onNavigateBack = { backStack.pop() },
            onOpenProjectDetails = { singleProject, projectDetails ->
                backStack.updateSingleProjectState(
                    singleProject = singleProject,
                    projectDetails = projectDetails
                )
                backStack.add(
                    element = Screen.ProjectDetails(
                        projectDetails = projectDetails ?: ProjectDetailsState()
                            .copy(
                                date = singleProject.date,
                                projectName = singleProject.projectName,
                                projectTime = singleProject.projectTime
                            )
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
    projectDetails: ProjectDetailsState,
    settings: SettingsState
) {
    pop()
    val currentLast = lastOrNull()
    if (currentLast is Screen.SingleProject) {
        this[size - 1] = currentLast.copy(
            projectTime = projectDetails.projectTime,
            projectDetails = projectDetails,
            settingsEstimates = settings
        )
    }
}

internal fun SnapshotStateList<Any>.updateSingleProjectState(
    singleProject: SingleProjectState,
    projectDetails: ProjectDetailsState?
) {
    val index = size - 1
    val current = getOrNull(index = index)
    if (current is Screen.SingleProject) {
        this[index] = current.copy(
            projectName = singleProject.projectName,
            projectTime = singleProject.projectTime,
            kilometres = singleProject.kilometres,
            allowance = singleProject.allowance,
            workType = singleProject.workType,
            projectDetails = projectDetails
        )
    }
}

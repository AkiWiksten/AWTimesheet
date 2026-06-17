package com.akiwiksten.awtimesheet.navigation

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import com.akiwiksten.awtimesheet.feature.location.LocationPickerScreen
import com.akiwiksten.awtimesheet.feature.location.LocationScreen
import com.akiwiksten.awtimesheet.feature.location.LocationPickerResult
import com.akiwiksten.awtimesheet.feature.projectdetails.ProjectDetailsScreen
import com.akiwiksten.awtimesheet.feature.singleproject.SingleProjectScreen
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectNavigationActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectRouteArgs
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt
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
internal fun LocationEntry(screen: Screen.Location, backStack: SnapshotStateList<Any>) {
    val distanceKm = remember(screen.startPoint, screen.destinationPoint) {
        screen.startPoint?.let { start ->
            screen.destinationPoint?.let { destination ->
                calculateDistanceKm(start = start, destination = destination)
            }
        }
    }

    LocationScreen(
        startAddress = screen.startPoint?.address,
        destinationAddress = screen.destinationPoint?.address,
        distanceKm = distanceKm,
        onSelectStartPoint = {
            backStack.add(element = Screen.LocationPicker(target = Screen.LocationTarget.START))
        },
        onSelectDestinationPoint = {
            backStack.add(element = Screen.LocationPicker(target = Screen.LocationTarget.DESTINATION))
        },
        onNavigateBack = {
            distanceKm?.let { kilometres ->
                backStack.updateSingleProjectKilometres(kilometres = formatDistanceForSingleProject(kilometres))
            }
            backStack.pop()
        }
    )
}

@Composable
internal fun LocationPickerEntry(screen: Screen.LocationPicker, backStack: SnapshotStateList<Any>) {
    LocationPickerScreen(
        onLocationSelected = { result ->
            backStack.updateLocationSelection(target = screen.target, result = result)
            backStack.pop()
        },
        onNavigateBack = { backStack.pop() }
    )
}

@Composable
internal fun ProjectDetailsEntry(screen: Screen.ProjectDetails, backStack: SnapshotStateList<Any>) {
    ProjectDetailsScreen(
        detailsArgs = ProjectDetailsState(
            date = screen.date,
            projectName = screen.projectName,
            projectTime = screen.projectTime,
            startTime = screen.startTime,
            endTime = screen.endTime,
            lunchStart = screen.lunchStart,
            lunchEnd = screen.lunchEnd,
            breakStart = screen.breakStart,
            breakEnd = screen.breakEnd,
        ),
        onNavigateBack = { backStack.pop() },
        onConfirm = { details ->
            backStack.updateSingleProjectWorkTime(
                details = Screen.ProjectDetails(
                    date = details.date,
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
            kilometres = screen.kilometres,
            allowance = screen.allowance,
            workType = screen.workType,
            comment = screen.comment,
            projectDetails = if (screen.details == null) {
                null
            } else {
                ProjectDetailsState(
                    date = screen.details.date,
                    projectName = screen.details.projectName,
                    projectTime = screen.details.projectTime,
                    startTime = screen.details.startTime,
                    endTime = screen.details.endTime,
                    lunchStart = screen.details.lunchStart,
                    lunchEnd = screen.details.lunchEnd,
                    breakStart = screen.details.breakStart,
                    breakEnd = screen.details.breakEnd
                )
            }
        ),
        navigationActions = SingleProjectNavigationActions(
            onNavigateBack = { backStack.pop() },
            onOpenProjectDetails = { singleProject, projectDetails ->
                backStack.updateSingleProjectState(singleProject)
                backStack.add(
                    element = Screen.ProjectDetails(
                        date = projectDetails?.date ?: "",
                        projectTime = singleProject.projectTime,
                        projectName = singleProject.projectName,
                        startTime = projectDetails?.startTime ?: "",
                        endTime = projectDetails?.endTime ?: "",
                        lunchStart = projectDetails?.lunchStart ?: "",
                        lunchEnd = projectDetails?.lunchEnd ?: "",
                        breakStart = projectDetails?.breakStart ?: "",
                        breakEnd = projectDetails?.breakEnd ?: ""
                    )
                )
            },
            onNavigateToLocationPicker = { singleProject ->
                backStack.updateSingleProjectState(singleProject)
                backStack.add(element = Screen.Location())
            }
        )
    )
}

private fun calculateDistanceKm(
    start: Screen.LocationPoint,
    destination: Screen.LocationPoint
): Double {
    val earthRadiusKm = 6371.0
    val lat1 = Math.toRadians(start.latitude)
    val lat2 = Math.toRadians(destination.latitude)
    val deltaLat = Math.toRadians(destination.latitude - start.latitude)
    val deltaLon = Math.toRadians(destination.longitude - start.longitude)

    val haversine =
        kotlin.math.sin(deltaLat / 2) * kotlin.math.sin(deltaLat / 2) +
            kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
            kotlin.math.sin(deltaLon / 2) * kotlin.math.sin(deltaLon / 2)
    val arc = 2 * kotlin.math.atan2(kotlin.math.sqrt(haversine), kotlin.math.sqrt(1 - haversine))
    return earthRadiusKm * arc
}

private fun formatDistanceForSingleProject(distanceKm: Double): String {
    return distanceKm.roundToInt().toString()
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
            projectTime = details.projectTime,
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
            workType = singleProject.workType,
            comment = singleProject.comment
        )
    }
}

internal fun SnapshotStateList<Any>.updateSingleProjectKilometres(
    kilometres: String
) {
    val index = size - 1
    val current = getOrNull(index = index)
    if (current is Screen.SingleProject) {
        this[index] = current.copy(
            kilometres = kilometres
        )
    }
}

internal fun SnapshotStateList<Any>.updateLocationSelection(
    target: Screen.LocationTarget,
    result: LocationPickerResult
) {
    val index = (size - 1 downTo 0).firstOrNull { getOrNull(it) is Screen.Location } ?: return
    val current = getOrNull(index) as? Screen.Location ?: return
    val point = Screen.LocationPoint(
        latitude = result.latitude,
        longitude = result.longitude,
        address = result.address
    )

    this[index] = when (target) {
        Screen.LocationTarget.START -> current.copy(startPoint = point)
        Screen.LocationTarget.DESTINATION -> current.copy(destinationPoint = point)
    }
}


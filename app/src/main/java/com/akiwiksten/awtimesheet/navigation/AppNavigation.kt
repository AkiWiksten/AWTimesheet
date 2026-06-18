@file:Suppress("TooManyFunctions", "FunctionNaming")

package com.akiwiksten.awtimesheet.navigation

import android.os.Parcelable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.RouteState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.location.DistanceCalculatorScreen
import com.akiwiksten.awtimesheet.feature.location.DistanceCalculatorViewModel
import com.akiwiksten.awtimesheet.feature.location.LocationPickerScreen
import com.akiwiksten.awtimesheet.feature.location.LocationPickerResult
import com.akiwiksten.awtimesheet.feature.location.DistanceCalculatorScreenState
import com.akiwiksten.awtimesheet.feature.projectdetails.ProjectDetailsScreen
import com.akiwiksten.awtimesheet.feature.singleproject.SingleProjectScreen
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectNavigationActions
import com.akiwiksten.awtimesheet.feature.singleproject.model.SingleProjectRouteArgs
import kotlinx.parcelize.Parcelize
import kotlin.math.min
import kotlin.math.roundToInt

private const val EARTH_RADIUS_KM = 6371.0

@Parcelize
private class BackStackData(val items: ArrayList<Screen>) : Parcelable

@Parcelize
private data class LocationCardState(
    val startPoint: Screen.LocationPoint? = null,
    val destinationPoint: Screen.LocationPoint? = null,
    val distanceKm: Double? = null,
    val lastScreenStartPoint: Screen.LocationPoint? = null,
    val lastScreenDestinationPoint: Screen.LocationPoint? = null,
) : Parcelable {
    val startAddress: String? get() = startPoint?.address
    val destinationAddress: String? get() = destinationPoint?.address
}

private sealed interface LocationCardEvent {
    data class RouteSelected(val route: RouteState) : LocationCardEvent
    data class ScreenPointsChanged(
        val startPoint: Screen.LocationPoint?,
        val destinationPoint: Screen.LocationPoint?,
    ) : LocationCardEvent
}

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
internal fun LocationEntry(
    screen: Screen.Location,
    backStack: SnapshotStateList<Any>,
    viewModel: DistanceCalculatorViewModel = hiltViewModel(),
) {
    val routeHistory by viewModel.routeHistory.collectAsState()
    val selectedRoute by viewModel.selectedRoute.collectAsState()

    val initialCardState = LocationCardState(
        startPoint = screen.startPoint,
        destinationPoint = screen.destinationPoint,
        distanceKm = if (screen.startPoint != null && screen.destinationPoint != null) {
            calculateDistanceKm(start = screen.startPoint, destination = screen.destinationPoint)
        } else {
            null
        },
        lastScreenStartPoint = screen.startPoint,
        lastScreenDestinationPoint = screen.destinationPoint,
    )
    val cardState = rememberSaveable { mutableStateOf(initialCardState) }

    LaunchedEffect(selectedRoute) {
        val route = selectedRoute
        if (route != null) {
            cardState.value = reduceLocationCardState(
                current = cardState.value,
                event = LocationCardEvent.RouteSelected(route = route)
            )
        }
    }

    LaunchedEffect(screen.startPoint, screen.destinationPoint) {
        cardState.value = reduceLocationCardState(
            current = cardState.value,
            event = LocationCardEvent.ScreenPointsChanged(
                startPoint = screen.startPoint,
                destinationPoint = screen.destinationPoint,
            )
        )
    }

    val currentCardState = cardState.value
    val cardStartPoint = currentCardState.startPoint
    val cardDestinationPoint = currentCardState.destinationPoint
    val cardStartAddress = currentCardState.startAddress
    val cardDestinationAddress = currentCardState.destinationAddress
    val cardDistanceKm = currentCardState.distanceKm
    val cardConfirmDistance = when (val distance = cardDistanceKm) {
        null -> null
        else -> distance.roundToInt().toString()
    }

    DistanceCalculatorScreen(
        state = DistanceCalculatorScreenState(
            startAddress = cardStartAddress,
            destinationAddress = cardDestinationAddress,
            distanceKm = cardDistanceKm,
            routeHistory = routeHistory,
            selectedRoute = selectedRoute,
            onClearRouteHistory = { viewModel.clearRouteHistory() },
            onRouteSelected = viewModel::selectRoute,
            onSelectStartPoint = {
                viewModel.clearSelectedRoute()
                backStack.seedLocationPointsFromCard(
                    startPoint = cardStartPoint,
                    destinationPoint = cardDestinationPoint,
                )
                backStack.add(
                    element = Screen.LocationPicker(
                        target = Screen.LocationTarget.START,
                        initialPoint = cardStartPoint,
                    )
                )
            },
            onSelectDestinationPoint = {
                viewModel.clearSelectedRoute()
                backStack.seedLocationPointsFromCard(
                    startPoint = cardStartPoint,
                    destinationPoint = cardDestinationPoint,
                )
                backStack.add(
                    element = Screen.LocationPicker(
                        target = Screen.LocationTarget.DESTINATION,
                        initialPoint = cardDestinationPoint,
                    )
                )
            },
            onAddToList = { kilometres ->
                val startAddress = cardStartAddress
                val destinationAddress = cardDestinationAddress
                val distanceToSave = cardConfirmDistance ?: kilometres
                if (startAddress != null && destinationAddress != null) {

                    viewModel.insertRoute(
                        distanceKm = distanceToSave,
                        startAddress = startAddress,
                        startLatitude = cardStartPoint?.latitude,
                        startLongitude = cardStartPoint?.longitude,
                        destinationAddress = destinationAddress,
                        destinationLatitude = cardDestinationPoint?.latitude,
                        destinationLongitude = cardDestinationPoint?.longitude,
                    )
                }
                backStack.confirmLocationDistance(distanceToSave)
            },
            onReturnDistance = {
                val distanceToReturn = selectedRoute?.distance?.removeSuffix(" km")
                if (distanceToReturn != null) {
                    backStack.confirmLocationDistance(distanceToReturn)
                    backStack.pop()
                }
            },
            onDeleteSelectedRoute = {
                selectedRoute?.let { route ->
                    viewModel.deleteRoute(route)
                    viewModel.clearSelectedRoute()
                }
            },
            onNavigateBack = {
                backStack.pop()
            }
        )
    )
}

@Composable
internal fun LocationPickerEntry(
    screen: Screen.LocationPicker,
    backStack: SnapshotStateList<Any>,
) {
    LocationPickerScreen(
        initialAddress = screen.initialPoint?.address,
        initialLatLng = screen.initialPoint?.let { point ->
            com.google.android.gms.maps.model.LatLng(point.latitude, point.longitude)
        },
        onLocationSelected = { result ->
            backStack.updateLocationSelection(target = screen.target, result = result)
            backStack.pop()
        },
        onNavigateBack = { backStack.pop() }
    )
}

private fun RouteState.toLocationPoint(target: Screen.LocationTarget): Screen.LocationPoint? {
    return when (target) {
        Screen.LocationTarget.START -> {
            val latitude = startLatitude
            val longitude = startLongitude
            if (latitude != null && longitude != null) {
                Screen.LocationPoint(
                    latitude = latitude,
                    longitude = longitude,
                    address = start,
                )
            } else {
                null
            }
        }

        Screen.LocationTarget.DESTINATION -> {
            val latitude = destinationLatitude
            val longitude = destinationLongitude
            if (latitude != null && longitude != null) {
                Screen.LocationPoint(
                    latitude = latitude,
                    longitude = longitude,
                    address = destination,
                )
            } else {
                null
            }
        }
    }
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
    val lat1 = Math.toRadians(start.latitude)
    val lat2 = Math.toRadians(destination.latitude)
    val deltaLat = Math.toRadians(destination.latitude - start.latitude)
    val deltaLon = Math.toRadians(destination.longitude - start.longitude)

    val haversine =
        kotlin.math.sin(deltaLat / 2) * kotlin.math.sin(deltaLat / 2) +
            kotlin.math.cos(lat1) * kotlin.math.cos(lat2) *
            kotlin.math.sin(deltaLon / 2) * kotlin.math.sin(deltaLon / 2)
    val arc = 2 * kotlin.math.atan2(kotlin.math.sqrt(haversine), kotlin.math.sqrt(1 - haversine))
    return EARTH_RADIUS_KM * arc
}

private fun reduceLocationCardState(
    current: LocationCardState,
    event: LocationCardEvent,
): LocationCardState {
    return when (event) {
        is LocationCardEvent.RouteSelected -> {
            val startPoint = event.route.toLocationPoint(target = Screen.LocationTarget.START)
            val destinationPoint = event.route.toLocationPoint(target = Screen.LocationTarget.DESTINATION)
            current.copy(
                startPoint = startPoint,
                destinationPoint = destinationPoint,
                distanceKm = event.route.distance.removeSuffix(" km").toDoubleOrNull(),
            )
        }

        is LocationCardEvent.ScreenPointsChanged -> {
            var next = current
            if (event.startPoint != null && event.startPoint != current.lastScreenStartPoint) {
                next = next.copy(
                    startPoint = event.startPoint,
                    lastScreenStartPoint = event.startPoint,
                )
            }
            if (
                event.destinationPoint != null &&
                event.destinationPoint != current.lastScreenDestinationPoint
            ) {
                next = next.copy(
                    destinationPoint = event.destinationPoint,
                    lastScreenDestinationPoint = event.destinationPoint,
                )
            }

            val start = next.startPoint
            val destination = next.destinationPoint
            if (start != null && destination != null) {
                next.copy(distanceKm = calculateDistanceKm(start = start, destination = destination))
            } else {
                next
            }
        }
    }
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
    val index = (size - 1 downTo 0).firstOrNull { getOrNull(it) is Screen.SingleProject } ?: return
    val current = getOrNull(index = index) as? Screen.SingleProject ?: return
    this[index] = current.copy(
        kilometres = kilometres
    )
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

internal fun SnapshotStateList<Any>.seedLocationPointsFromCard(
    startPoint: Screen.LocationPoint?,
    destinationPoint: Screen.LocationPoint?,
) {
    val index = (size - 1 downTo 0).firstOrNull { getOrNull(it) is Screen.Location } ?: return
    val current = getOrNull(index) as? Screen.Location ?: return

    this[index] = current.copy(
        startPoint = startPoint ?: current.startPoint,
        destinationPoint = destinationPoint ?: current.destinationPoint,
    )
}

internal fun SnapshotStateList<Any>.confirmLocationDistance(kilometres: String) {
    val index = (size - 1 downTo 0).firstOrNull { getOrNull(it) is Screen.Location } ?: return
    val locationScreen = getOrNull(index) as? Screen.Location ?: return

    // Clear current selection after a route is confirmed.
    this[index] = locationScreen.copy(
        startPoint = null,
        destinationPoint = null,
    )
    
    // Update SingleProject with the distance
    updateSingleProjectKilometres(kilometres = kilometres)
}


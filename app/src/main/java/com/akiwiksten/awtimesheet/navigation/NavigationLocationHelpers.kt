package com.akiwiksten.awtimesheet.navigation

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.akiwiksten.awtimesheet.domain.model.RouteState
import com.akiwiksten.awtimesheet.feature.location.DistanceCalculatorScreenState
import com.akiwiksten.awtimesheet.feature.location.DistanceCalculatorViewModel
import com.akiwiksten.awtimesheet.feature.location.InsertRouteRequest
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt

private const val EARTH_RADIUS_KM = 6371.0

@Parcelize
internal data class LocationCardState(
    val startPoint: Screen.LocationPoint? = null,
    val destinationPoint: Screen.LocationPoint? = null,
    val distanceKm: Double? = null,
    val isRoundTrip: Boolean = false,
    val lastScreenStartPoint: Screen.LocationPoint? = null,
    val lastScreenDestinationPoint: Screen.LocationPoint? = null,
) : Parcelable {
    val startAddress: String? get() = startPoint?.address
    val destinationAddress: String? get() = destinationPoint?.address
}
internal sealed interface LocationCardEvent {
    data class RouteSelected(val route: RouteState) : LocationCardEvent
    data class TripTypeChanged(val isRoundTrip: Boolean) : LocationCardEvent
    data class ScreenPointsChanged(
        val startPoint: Screen.LocationPoint?,
        val destinationPoint: Screen.LocationPoint?,
    ) : LocationCardEvent
}

internal data class DistanceCalculatorCardUiState(
    val startPoint: Screen.LocationPoint?,
    val destinationPoint: Screen.LocationPoint?,
    val startAddress: String?,
    val destinationAddress: String?,
    val distanceKm: Double?,
    val isRoundTrip: Boolean,
    val confirmDistance: String?,
)

internal fun LocationCardState.toDistanceCalculatorCardUiState(): DistanceCalculatorCardUiState {
    val roundedDistance = distanceKm?.roundToInt()?.toString()
    return DistanceCalculatorCardUiState(
        startPoint = startPoint,
        destinationPoint = destinationPoint,
        startAddress = startAddress,
        destinationAddress = destinationAddress,
        distanceKm = distanceKm,
        isRoundTrip = isRoundTrip,
        confirmDistance = roundedDistance,
    )
}

@Composable
internal fun rememberLocationCardState(
    screen: Screen.Location,
    selectedRoute: RouteState?,
) = rememberSaveable {
    mutableStateOf(
        LocationCardState(
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
    )
}.also { cardState ->
    LaunchedEffect(selectedRoute) {
        val route = selectedRoute ?: return@LaunchedEffect
        cardState.value = reduceLocationCardState(
            current = cardState.value,
            event = LocationCardEvent.RouteSelected(route = route)
        )
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
}

internal fun createDistanceCalculatorScreenState(
    backStack: SnapshotStateList<Any>,
    viewModel: DistanceCalculatorViewModel,
    routeHistory: List<RouteState>,
    selectedRoute: RouteState?,
    cardUiState: DistanceCalculatorCardUiState,
    onTripTypeChange: (Boolean) -> Unit,
): DistanceCalculatorScreenState {
    return DistanceCalculatorScreenState(
        startAddress = cardUiState.startAddress,
        destinationAddress = cardUiState.destinationAddress,
        distanceKm = cardUiState.distanceKm,
        isRoundTrip = cardUiState.isRoundTrip,
        routeHistory = routeHistory,
        selectedRoute = selectedRoute,
        onTripTypeChange = onTripTypeChange,
        onClearRouteHistory = { viewModel.clearRouteHistory() },
        onRouteSelected = viewModel::selectRoute,
        onSelectStartPoint = {
            navigateToLocationPicker(
                backStack = backStack,
                viewModel = viewModel,
                cardUiState = cardUiState,
                target = Screen.LocationTarget.START,
            )
        },
        onSelectDestinationPoint = {
            navigateToLocationPicker(
                backStack = backStack,
                viewModel = viewModel,
                cardUiState = cardUiState,
                target = Screen.LocationTarget.DESTINATION,
            )
        },
        onAddToList = { kilometres ->
            addCurrentCardRouteToHistory(
                backStack = backStack,
                viewModel = viewModel,
                cardUiState = cardUiState,
                fallbackDistance = kilometres,
            )
        },
        onReturnDistance = { returnSelectedRouteDistance(backStack = backStack, selectedRoute = selectedRoute) },
        onDeleteSelectedRoute = { deleteSelectedRoute(viewModel = viewModel, selectedRoute = selectedRoute) },
        onNavigateBack = { backStack.pop() }
    )
}

private fun navigateToLocationPicker(
    backStack: SnapshotStateList<Any>,
    viewModel: DistanceCalculatorViewModel,
    cardUiState: DistanceCalculatorCardUiState,
    target: Screen.LocationTarget,
) {
    viewModel.clearSelectedRoute()
    backStack.seedLocationPointsFromCard(
        startPoint = cardUiState.startPoint,
        destinationPoint = cardUiState.destinationPoint,
    )
    val initialPoint = when (target) {
        Screen.LocationTarget.START -> cardUiState.startPoint
        Screen.LocationTarget.DESTINATION -> cardUiState.destinationPoint
    }
    backStack.add(element = Screen.LocationPicker(target = target, initialPoint = initialPoint))
}

private fun addCurrentCardRouteToHistory(
    backStack: SnapshotStateList<Any>,
    viewModel: DistanceCalculatorViewModel,
    cardUiState: DistanceCalculatorCardUiState,
    fallbackDistance: String,
) {
    val startAddress = cardUiState.startAddress
    val destinationAddress = cardUiState.destinationAddress
    val distanceToSave = if (cardUiState.isRoundTrip) {
        "${cardUiState.confirmDistance} *2"
    } else {
        cardUiState.confirmDistance ?: fallbackDistance
    }

    if (startAddress != null && destinationAddress != null) {
        viewModel.insertRoute(
            request = InsertRouteRequest(
                distanceKm = distanceToSave,
                startAddress = startAddress,
                startLatitude = cardUiState.startPoint?.latitude,
                startLongitude = cardUiState.startPoint?.longitude,
                destinationAddress = destinationAddress,
                destinationLatitude = cardUiState.destinationPoint?.latitude,
                destinationLongitude = cardUiState.destinationPoint?.longitude,
            )
        )
    }
    backStack.confirmLocationDistance(distanceToSave)
}

private fun returnSelectedRouteDistance(backStack: SnapshotStateList<Any>, selectedRoute: RouteState?) {
    val distanceToReturn = selectedRoute?.distance?.let { distance ->
        if (distance.contains("*2")) {
            val numericPart = distance.substringBefore(" *2").removeSuffix(" km")
            val doubled = (numericPart.toDoubleOrNull() ?: 0.0) * 2
            doubled.roundToInt().toString()
        } else {
            distance.removeSuffix(" km")
        }
    } ?: return
    backStack.confirmLocationDistance(distanceToReturn)
    backStack.pop()
}

private fun deleteSelectedRoute(viewModel: DistanceCalculatorViewModel, selectedRoute: RouteState?) {
    selectedRoute?.let { route ->
        viewModel.deleteRoute(route)
        viewModel.clearSelectedRoute()
    }
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

internal fun reduceLocationCardState(
    current: LocationCardState,
    event: LocationCardEvent,
): LocationCardState {
    return when (event) {
        is LocationCardEvent.RouteSelected -> {
            val startPoint = event.route.toLocationPoint(target = Screen.LocationTarget.START)
            val destinationPoint = event.route.toLocationPoint(target = Screen.LocationTarget.DESTINATION)
            val isRoundTrip = event.route.distance.contains("*2")
            val distanceStr = event.route.distance.substringBefore(" *2").removeSuffix(" km")
            current.copy(
                startPoint = startPoint,
                destinationPoint = destinationPoint,
                distanceKm = distanceStr.toDoubleOrNull(),
                isRoundTrip = isRoundTrip
            )
        }

        is LocationCardEvent.TripTypeChanged -> {
            current.copy(isRoundTrip = event.isRoundTrip)
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

package com.akiwiksten.awtimesheet.feature.location

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.akiwiksten.awtimesheet.domain.model.RouteState
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt

private const val EARTH_RADIUS_KM = 6371.0

@Parcelize
internal data class LocationCardState(
    val startPoint: DistanceCalculatorLocationPoint? = null,
    val destinationPoint: DistanceCalculatorLocationPoint? = null,
    val distanceKm: Double? = null,
    val isRoundTrip: Boolean = false,
    val lastScreenStartPoint: DistanceCalculatorLocationPoint? = null,
    val lastScreenDestinationPoint: DistanceCalculatorLocationPoint? = null,
) : Parcelable {
    val startAddress: String? get() = startPoint?.address
    val destinationAddress: String? get() = destinationPoint?.address
}

internal sealed interface LocationCardEvent {
    data class RouteSelected(val route: RouteState) : LocationCardEvent
    data class TripTypeChanged(val isRoundTrip: Boolean) : LocationCardEvent
    data class ScreenPointsChanged(
        val startPoint: DistanceCalculatorLocationPoint?,
        val destinationPoint: DistanceCalculatorLocationPoint?,
    ) : LocationCardEvent
}

internal data class DistanceCalculatorCardUiState(
    val startPoint: DistanceCalculatorLocationPoint?,
    val destinationPoint: DistanceCalculatorLocationPoint?,
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
    initialStartPoint: DistanceCalculatorLocationPoint?,
    initialDestinationPoint: DistanceCalculatorLocationPoint?,
    selectedRoute: RouteState?,
) = rememberSaveable {
    mutableStateOf(
        LocationCardState(
            startPoint = initialStartPoint,
            destinationPoint = initialDestinationPoint,
            distanceKm = if (initialStartPoint != null && (initialDestinationPoint != null)) {
                calculateDistanceKm(start = initialStartPoint, destination = initialDestinationPoint)
            } else {
                null
            },
            lastScreenStartPoint = initialStartPoint,
            lastScreenDestinationPoint = initialDestinationPoint,
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
    LaunchedEffect(initialStartPoint, initialDestinationPoint) {
        cardState.value = reduceLocationCardState(
            current = cardState.value,
            event = LocationCardEvent.ScreenPointsChanged(
                startPoint = initialStartPoint,
                destinationPoint = initialDestinationPoint,
            )
        )
    }
}

internal fun reduceLocationCardState(
    current: LocationCardState,
    event: LocationCardEvent,
): LocationCardState {
    return when (event) {
        is LocationCardEvent.RouteSelected -> {
            val startPoint = event.route.toLocationPoint(isStart = true)
            val destinationPoint = event.route.toLocationPoint(isStart = false)
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

private fun RouteState.toLocationPoint(isStart: Boolean): DistanceCalculatorLocationPoint? {
    val latitude = if (isStart) startLatitude else destinationLatitude
    val longitude = if (isStart) startLongitude else destinationLongitude
    val address = if (isStart) start else destination
    
    return if (latitude != null && longitude != null) {
        DistanceCalculatorLocationPoint(
            latitude = latitude,
            longitude = longitude,
            address = address,
        )
    } else {
        null
    }
}

private fun calculateDistanceKm(
    start: DistanceCalculatorLocationPoint,
    destination: DistanceCalculatorLocationPoint
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

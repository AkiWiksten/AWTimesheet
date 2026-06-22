package com.akiwiksten.awtimesheet.feature.location

import android.os.Parcelable
import com.akiwiksten.awtimesheet.domain.model.RouteState
import kotlinx.parcelize.Parcelize

@Parcelize
data class DistanceCalculatorLocationPoint(
    val latitude: Double,
    val longitude: Double,
    val address: String
) : Parcelable

data class DistanceCalculatorScreenState(
    val startAddress: String?,
    val destinationAddress: String?,
    val distanceKm: Double?,
    val isRoundTrip: Boolean = false,
    val routeHistory: List<RouteState> = emptyList(),
    val selectedRoute: RouteState? = null,
    val onTripTypeChange: (Boolean) -> Unit = {},
    val onClearRouteHistory: () -> Unit,
    val onRouteSelected: (RouteState) -> Unit,
    val onSelectStartPoint: () -> Unit,
    val onSelectDestinationPoint: () -> Unit,
    val onAddToList: (String) -> Unit,
    val onReturnDistance: () -> Unit,
    val onDeleteSelectedRoute: () -> Unit,
    val onNavigateBack: () -> Unit
)

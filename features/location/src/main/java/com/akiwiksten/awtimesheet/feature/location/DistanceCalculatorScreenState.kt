package com.akiwiksten.awtimesheet.feature.location

import com.akiwiksten.awtimesheet.domain.model.RouteState

data class DistanceCalculatorScreenState(
    val startAddress: String?,
    val destinationAddress: String?,
    val distanceKm: Double?,
    val routeHistory: List<RouteState> = emptyList(),
    val selectedRoute: RouteState? = null,
    val onClearRouteHistory: () -> Unit,
    val onRouteSelected: (RouteState) -> Unit,
    val onSelectStartPoint: () -> Unit,
    val onSelectDestinationPoint: () -> Unit,
    val onAddToList: (String) -> Unit,
    val onReturnDistance: () -> Unit,
    val onDeleteSelectedRoute: () -> Unit,
    val onNavigateBack: () -> Unit
)

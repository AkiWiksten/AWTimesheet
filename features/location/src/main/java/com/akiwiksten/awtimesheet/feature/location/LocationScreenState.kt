package com.akiwiksten.awtimesheet.feature.location

data class LocationScreenState(
    val startAddress: String?,
    val destinationAddress: String?,
    val distanceKm: Double?,
    val onSelectStartPoint: () -> Unit,
    val onSelectDestinationPoint: () -> Unit,
    val onConfirmDistance: (String) -> Unit,
    val onNavigateBack: () -> Unit
)


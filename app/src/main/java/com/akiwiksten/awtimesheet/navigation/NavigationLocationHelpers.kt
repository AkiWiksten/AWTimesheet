package com.akiwiksten.awtimesheet.navigation

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.akiwiksten.awtimesheet.feature.location.DistanceCalculatorLocationPoint

internal fun navigateToLocationPicker(
    backStack: SnapshotStateList<Any>,
    startPoint: DistanceCalculatorLocationPoint?,
    destinationPoint: DistanceCalculatorLocationPoint?,
    target: Screen.LocationTarget,
) {
    backStack.seedLocationPointsFromCard(
        startPoint = startPoint?.toScreenLocationPoint(),
        destinationPoint = destinationPoint?.toScreenLocationPoint(),
    )
    val initialPoint = when (target) {
        Screen.LocationTarget.START -> startPoint
        Screen.LocationTarget.DESTINATION -> destinationPoint
    }
    backStack.add(
        element = Screen.LocationPicker(
            target = target,
            initialPoint = initialPoint?.toScreenLocationPoint()
        )
    )
}

internal fun DistanceCalculatorLocationPoint.toScreenLocationPoint(): Screen.LocationPoint {
    return Screen.LocationPoint(
        latitude = latitude,
        longitude = longitude,
        address = address
    )
}

internal fun Screen.LocationPoint.toFeatureLocationPoint(): DistanceCalculatorLocationPoint {
    return DistanceCalculatorLocationPoint(
        latitude = latitude,
        longitude = longitude,
        address = address
    )
}

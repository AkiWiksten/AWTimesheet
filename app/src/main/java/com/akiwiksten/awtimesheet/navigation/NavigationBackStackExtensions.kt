package com.akiwiksten.awtimesheet.navigation

import androidx.compose.runtime.snapshots.SnapshotStateList
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.feature.location.LocationPickerResult

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
            projectName = details.projectName,
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
    val index = (size - 1 downTo 0).firstOrNull { getOrNull(it) is Screen.DistanceCalculator } ?: return
    val current = getOrNull(index) as? Screen.DistanceCalculator ?: return
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
    val index = (size - 1 downTo 0).firstOrNull { getOrNull(it) is Screen.DistanceCalculator } ?: return
    val current = getOrNull(index) as? Screen.DistanceCalculator ?: return

    this[index] = current.copy(
        startPoint = startPoint ?: current.startPoint,
        destinationPoint = destinationPoint ?: current.destinationPoint,
    )
}

internal fun SnapshotStateList<Any>.confirmLocationDistance(kilometres: String) {
    val index = (size - 1 downTo 0).firstOrNull { getOrNull(it) is Screen.DistanceCalculator } ?: return
    val locationScreen = getOrNull(index) as? Screen.DistanceCalculator ?: return

    this[index] = locationScreen.copy(
        startPoint = null,
        destinationPoint = null,
    )
    updateSingleProjectKilometres(kilometres = kilometres)
}

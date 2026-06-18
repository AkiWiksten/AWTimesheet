package com.akiwiksten.awtimesheet.data.mapper

import com.akiwiksten.awtimesheet.data.database.entity.RouteEntity
import com.akiwiksten.awtimesheet.domain.model.RouteState

fun RouteEntity.toDomain(): RouteState {
    return RouteState(
        timestamp = timestamp,
        start = startPoint,
        startLatitude = startLatitude,
        startLongitude = startLongitude,
        destination = destinationPoint,
        destinationLatitude = destinationLatitude,
        destinationLongitude = destinationLongitude,
        distance = distance,
    )
}

fun RouteState.toEntity(): RouteEntity {
    return RouteEntity(
        timestamp = timestamp,
        startPoint = start,
        startLatitude = startLatitude,
        startLongitude = startLongitude,
        destinationPoint = destination,
        destinationLatitude = destinationLatitude,
        destinationLongitude = destinationLongitude,
        distance = distance,
    )
}

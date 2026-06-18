package com.akiwiksten.awtimesheet.data.mapper

import com.akiwiksten.awtimesheet.data.database.entity.RouteEntity
import com.akiwiksten.awtimesheet.domain.model.RouteState

fun RouteEntity.toDomain(): RouteState {
    return RouteState(
        timestamp = timestamp,
        start = startPoint,
        destination = destinationPoint,
        distance = distance,
    )
}

fun RouteState.toEntity(): RouteEntity {
    return RouteEntity(
        timestamp = timestamp,
        startPoint = start,
        destinationPoint = destination,
        distance = distance,
    )
}

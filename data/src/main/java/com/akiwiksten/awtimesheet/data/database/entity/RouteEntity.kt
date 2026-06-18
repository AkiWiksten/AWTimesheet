package com.akiwiksten.awtimesheet.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.akiwiksten.awtimesheet.core.DESTINATION_POINT
import com.akiwiksten.awtimesheet.core.DISTANCE
import com.akiwiksten.awtimesheet.core.ROUTE_TABLE
import com.akiwiksten.awtimesheet.core.START_POINT
import com.akiwiksten.awtimesheet.core.TIMESTAMP
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = ROUTE_TABLE,
    primaryKeys = [START_POINT, DESTINATION_POINT]
)
data class RouteEntity(
    @ColumnInfo(name = TIMESTAMP) val timestamp: String = "",
    @ColumnInfo(name = START_POINT) val startPoint: String = "",
    @ColumnInfo(name = DESTINATION_POINT) val destinationPoint: String = "",
    @ColumnInfo(name = DISTANCE) val distance: String = "",
)
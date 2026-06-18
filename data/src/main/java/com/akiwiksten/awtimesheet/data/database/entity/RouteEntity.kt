package com.akiwiksten.awtimesheet.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.akiwiksten.awtimesheet.core.DESTINATION_POINT
import com.akiwiksten.awtimesheet.core.DESTINATION_LATITUDE
import com.akiwiksten.awtimesheet.core.DESTINATION_LONGITUDE
import com.akiwiksten.awtimesheet.core.DISTANCE
import com.akiwiksten.awtimesheet.core.ROUTE_TABLE
import com.akiwiksten.awtimesheet.core.START_POINT
import com.akiwiksten.awtimesheet.core.START_LATITUDE
import com.akiwiksten.awtimesheet.core.START_LONGITUDE
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
    @ColumnInfo(name = START_LATITUDE) val startLatitude: Double? = null,
    @ColumnInfo(name = START_LONGITUDE) val startLongitude: Double? = null,
    @ColumnInfo(name = DESTINATION_POINT) val destinationPoint: String = "",
    @ColumnInfo(name = DESTINATION_LATITUDE) val destinationLatitude: Double? = null,
    @ColumnInfo(name = DESTINATION_LONGITUDE) val destinationLongitude: Double? = null,
    @ColumnInfo(name = DISTANCE) val distance: String = "",
)
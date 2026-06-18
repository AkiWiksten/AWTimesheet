package com.akiwiksten.awtimesheet.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RouteState(
    val distance: String,
    val start: String,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val destination: String,
    val destinationLatitude: Double? = null,
    val destinationLongitude: Double? = null,
    val timestamp: String = System.currentTimeMillis().toString(),
): Parcelable
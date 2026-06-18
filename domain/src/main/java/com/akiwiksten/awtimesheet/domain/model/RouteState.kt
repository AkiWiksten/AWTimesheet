package com.akiwiksten.awtimesheet.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RouteState(
    val distance: String,
    val start: String,
    val destination: String,
    val timestamp: String = System.currentTimeMillis().toString(),
): Parcelable
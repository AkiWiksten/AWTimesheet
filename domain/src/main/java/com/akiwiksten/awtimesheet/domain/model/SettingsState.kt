package com.akiwiksten.awtimesheet.domain.model

import android.os.Parcelable
import com.akiwiksten.awtimesheet.core.ZERO_TIME
import kotlinx.parcelize.Parcelize

@Parcelize
data class SettingsState(
    val name: String = "",
    val employer: String = "",
    val dailyWorkTimeEstimate: String = ZERO_TIME,
    val dailyLunchTimeEstimate: String = ZERO_TIME,
    val initialFlexTimeTotal: String = ZERO_TIME,
    val calculatedFlexTimeTotal: String = ZERO_TIME,
    val workTypes: List<String> = emptyList(),
    val enableTestFeatures: Boolean = false,
) : Parcelable

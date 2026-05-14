package com.akiwiksten.worktime30.domain.model

import android.os.Parcelable
import com.akiwiksten.worktime30.core.ZERO_TIME
import kotlinx.parcelize.Parcelize

@Parcelize
data class SettingsState(
    val name: String = "",
    val employer: String = "",
    val dailyWorkTimeEstimate: String = "",
    val dailyLunchTimeEstimate: String = ZERO_TIME,
    val initialFlexTimeTotal: String = ZERO_TIME,
    val workTypes: List<String> = emptyList()
) : Parcelable

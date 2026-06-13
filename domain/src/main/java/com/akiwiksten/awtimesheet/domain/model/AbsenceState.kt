package com.akiwiksten.awtimesheet.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AbsenceState(
    val absenceType: String = "",
    val startDate: String = "",
    val endDate: String = "",
) : Parcelable

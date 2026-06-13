package com.akiwiksten.awtimesheet.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.awtimesheet.core.ABSENCE_TABLE
import com.akiwiksten.awtimesheet.core.ABSENCE_TYPE
import com.akiwiksten.awtimesheet.core.END_DATE
import com.akiwiksten.awtimesheet.core.HAS_WEEKENDS
import com.akiwiksten.awtimesheet.core.ID
import com.akiwiksten.awtimesheet.core.START_DATE
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = ABSENCE_TABLE)
data class AbsenceEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = ID) val id: Int = 0,
    @ColumnInfo(name = START_DATE) val startDate: String = "",
    @ColumnInfo(name = END_DATE) val endDate: String = "",
    @ColumnInfo(name = ABSENCE_TYPE) val absenceType: String = "",
    @ColumnInfo(name = HAS_WEEKENDS) val hasWeekends: Boolean = false,
)

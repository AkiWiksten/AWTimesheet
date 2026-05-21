package com.akiwiksten.awtimesheet.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.awtimesheet.core.CALCULATED_FLEXTIME_TOTAL_TABLE
import com.akiwiksten.awtimesheet.core.CALCULATED_FLEX_TIME_TOTAL
import com.akiwiksten.awtimesheet.core.ID
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = CALCULATED_FLEXTIME_TOTAL_TABLE)
data class CalculatedFlextimeTotalEntity(
    @PrimaryKey @ColumnInfo(name = ID) val id: Int = 1,
    @ColumnInfo(name = CALCULATED_FLEX_TIME_TOTAL) val calculatedFlexTimeTotal: String = "",
)
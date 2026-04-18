package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.worktime30.core.BALANCE_TOTAL
import com.akiwiksten.worktime30.core.DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.ID
import com.akiwiksten.worktime30.core.LUNCH_TIME
import com.akiwiksten.worktime30.core.WORK_STATS_TABLE
import com.akiwiksten.worktime30.core.WORK_TIME_TOTAL
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = WORK_STATS_TABLE)
data class WorkStatsEntity(
    @PrimaryKey @ColumnInfo(name = ID) val id: Int = 1,
    @ColumnInfo(name = DAILY_WORK_TIME) val dailyWorkTime: String = "",
    @ColumnInfo(name = LUNCH_TIME) val lunchTime: String = "",
    @ColumnInfo(name = WORK_TIME_TOTAL) val workTimeTotal: String = "",
    @ColumnInfo(name = BALANCE_TOTAL) val balanceTotal: String = "",
)

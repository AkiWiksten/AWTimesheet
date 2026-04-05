package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.worktime30.core.BALANCE_TODAY
import com.akiwiksten.worktime30.core.BALANCE_TOTAL
import com.akiwiksten.worktime30.core.BREAK_END
import com.akiwiksten.worktime30.core.BREAK_START
import com.akiwiksten.worktime30.core.DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.END_TIME
import com.akiwiksten.worktime30.core.LUNCH_END
import com.akiwiksten.worktime30.core.LUNCH_START
import com.akiwiksten.worktime30.core.LUNCH_TIME
import com.akiwiksten.worktime30.core.START_TIME
import com.akiwiksten.worktime30.core.WORK_TIME_TODAY
import com.akiwiksten.worktime30.core.WORK_TIME_TOTAL
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "workday")
data class WorkDayEntity(
    @PrimaryKey val date: String,
    @ColumnInfo(name = START_TIME) val startTime: String = "",
    @ColumnInfo(name = END_TIME) val endTime: String = "",
    @ColumnInfo(name = LUNCH_START) val lunchStart: String = "",
    @ColumnInfo(name = LUNCH_END) val lunchEnd: String = "",
    @ColumnInfo(name = BREAK_START) val breakStart: String = "",
    @ColumnInfo(name = BREAK_END) val breakEnd: String = "",
    @ColumnInfo(name = WORK_TIME_TODAY) val workTimeToday: String = "",
    @ColumnInfo(name = BALANCE_TODAY) val balanceToday: String = "",
)

@Serializable
@Entity(tableName = "workdayonerow")
data class WorkDayOneRowEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = DAILY_WORK_TIME) val dailyWorkTime: String = "",
    @ColumnInfo(name = LUNCH_TIME) val lunchTime: String = "",
    @ColumnInfo(name = WORK_TIME_TOTAL) val workTimeTotal: String = "",
    @ColumnInfo(name = BALANCE_TOTAL) val balanceTotal: String = "",
)

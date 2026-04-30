package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.worktime30.core.DATE
import com.akiwiksten.worktime30.core.DEFAULT_DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.WORKDAY_TABLE
import com.akiwiksten.worktime30.core.WORK_TIME_BY_DATE_ESTIMATE
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = WORKDAY_TABLE)
data class WorkdayEntity(
    @PrimaryKey @ColumnInfo(name = DATE) val date: String,
    @ColumnInfo(name = WORK_TIME_BY_DATE_ESTIMATE) val workTimeByDateEstimate: String = DEFAULT_DAILY_WORK_TIME,
)

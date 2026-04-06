package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "workday",
    primaryKeys = ["date", "project_name"]
)
data class WorkDayEntity(
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "project_name") val projectName: String = "",
    @ColumnInfo(name = "start_time") val startTime: String = "",
    @ColumnInfo(name = "end_time") val endTime: String = "",
    @ColumnInfo(name = "lunch_start") val lunchStart: String = "",
    @ColumnInfo(name = "lunch_end") val lunchEnd: String = "",
    @ColumnInfo(name = "break_start") val breakStart: String = "",
    @ColumnInfo(name = "break_end") val breakEnd: String = "",
    @ColumnInfo(name = "work_time_today") val workTimeToday: String = "",
    @ColumnInfo(name = "balance_today") val balanceToday: String = "",
)

@Serializable
@Entity(tableName = "work_stats")
data class WorkStatsEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = 1,
    @ColumnInfo(name = "daily_work_time") val dailyWorkTime: String = "",
    @ColumnInfo(name = "lunch_time") val lunchTime: String = "",
    @ColumnInfo(name = "work_time_total") val workTimeTotal: String = "",
    @ColumnInfo(name = "balance_total") val balanceTotal: String = "",
)

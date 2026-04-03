package com.akiwiksten.worktime30.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.worktime30.core.ALLOWANCE
import com.akiwiksten.worktime30.core.BALANCE_TODAY
import com.akiwiksten.worktime30.core.BALANCE_TOTAL
import com.akiwiksten.worktime30.core.BREAK_END
import com.akiwiksten.worktime30.core.BREAK_START
import com.akiwiksten.worktime30.core.DAILY_WORK_TIME
import com.akiwiksten.worktime30.core.DATE
import com.akiwiksten.worktime30.core.EMPLOYER
import com.akiwiksten.worktime30.core.END_TIME
import com.akiwiksten.worktime30.core.KILOMETRES
import com.akiwiksten.worktime30.core.LUNCH_END
import com.akiwiksten.worktime30.core.LUNCH_START
import com.akiwiksten.worktime30.core.LUNCH_TIME
import com.akiwiksten.worktime30.core.NAME
import com.akiwiksten.worktime30.core.PROJECT_END_TIME
import com.akiwiksten.worktime30.core.PROJECT_NAME
import com.akiwiksten.worktime30.core.PROJECT_START_TIME
import com.akiwiksten.worktime30.core.PROJECT_TIME
import com.akiwiksten.worktime30.core.START_TIME
import com.akiwiksten.worktime30.core.WORK_TIME_TODAY
import com.akiwiksten.worktime30.core.WORK_TIME_TOTAL
import com.akiwiksten.worktime30.core.WORK_TYPE
import kotlinx.serialization.Serializable

@Serializable
@Entity
data class WorkDay(
    @PrimaryKey var date: String,
    @ColumnInfo(name = START_TIME) var startTime: String = "",
    @ColumnInfo(name = END_TIME) var endTime: String = "",
    @ColumnInfo(name = LUNCH_START) var lunchStart: String = "",
    @ColumnInfo(name = LUNCH_END) var lunchEnd: String = "",
    @ColumnInfo(name = BREAK_START) var breakStart: String = "",
    @ColumnInfo(name = BREAK_END) var breakEnd: String = "",
    @ColumnInfo(name = WORK_TIME_TODAY) var workTimeToday: String = "",
    @ColumnInfo(name = BALANCE_TODAY) var balanceToday: String = "",
)

@Serializable
@Entity
data class WorkDayOneRow(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = DAILY_WORK_TIME) var dailyWorkTime: String = "",
    @ColumnInfo(name = LUNCH_TIME) var lunchTime: String = "",
    @ColumnInfo(name = WORK_TIME_TOTAL) var workTimeTotal: String = "",
    @ColumnInfo(name = BALANCE_TOTAL) var balanceTotal: String = "",
)

@Serializable
@Entity(primaryKeys = [DATE, PROJECT_NAME])
data class Project(
    @ColumnInfo(name = DATE) var date: String,
    @ColumnInfo(name = PROJECT_NAME) var projectName: String = "",
    @ColumnInfo(name = PROJECT_START_TIME) var projectStartTime: String = "",
    @ColumnInfo(name = PROJECT_END_TIME) var projectEndTime: String = "",
    @ColumnInfo(name = PROJECT_TIME) var projectTime: String = "",
    @ColumnInfo(name = KILOMETRES) var kilometres: Int = 0,
    @ColumnInfo(name = ALLOWANCE) var allowance: String = "",
    @ColumnInfo(name = WORK_TYPE) var workType: String = "",
)

@Serializable
@Entity
data class ProjectName(
    @PrimaryKey var name: String,
)

@Serializable
@Entity
data class Settings(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = NAME) var name: String = "",
    @ColumnInfo(name = EMPLOYER) var employer: String = "",
)

@Serializable
@Entity
data class WorkType(
    @PrimaryKey var workType: String = "",
)

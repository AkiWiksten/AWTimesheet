package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.worktime30.core.DAILY_LUNCH_TIME_ESTIMATE
import com.akiwiksten.worktime30.core.DAILY_WORK_TIME_ESTIMATE
import com.akiwiksten.worktime30.core.EMPLOYER
import com.akiwiksten.worktime30.core.ID
import com.akiwiksten.worktime30.core.INITIAL_FLEX_TIME_TOTAL
import com.akiwiksten.worktime30.core.NAME
import com.akiwiksten.worktime30.core.SETTINGS_TABLE
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = SETTINGS_TABLE)
data class SettingsEntity(
    @PrimaryKey @ColumnInfo(name = ID) val id: Int = 1,
    @ColumnInfo(name = NAME) val name: String = "",
    @ColumnInfo(name = EMPLOYER) val employer: String = "",
    @ColumnInfo(name = DAILY_WORK_TIME_ESTIMATE) val dailyWorkTimeEstimate: String = "",
    @ColumnInfo(name = DAILY_LUNCH_TIME_ESTIMATE) val dailyLunchTimeEstimate: String = "",
    @ColumnInfo(name = INITIAL_FLEX_TIME_TOTAL) val initialFlexTimeTotal: String = "",
)

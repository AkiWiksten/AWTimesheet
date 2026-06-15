package com.akiwiksten.awtimesheet.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.awtimesheet.core.DAILY_LUNCH_TIME_ESTIMATE
import com.akiwiksten.awtimesheet.core.DAILY_WORK_TIME_ESTIMATE
import com.akiwiksten.awtimesheet.core.EMPLOYER
import com.akiwiksten.awtimesheet.core.ENABLE_TEST_FEATURES
import com.akiwiksten.awtimesheet.core.ID
import com.akiwiksten.awtimesheet.core.INITIAL_FLEX_TIME_TOTAL
import com.akiwiksten.awtimesheet.core.NAME
import com.akiwiksten.awtimesheet.core.SETTINGS_TABLE
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
    @ColumnInfo(name = ENABLE_TEST_FEATURES) val enableTestFeatures: Boolean = false,
)

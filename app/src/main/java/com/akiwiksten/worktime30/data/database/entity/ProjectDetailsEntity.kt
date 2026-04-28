package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.akiwiksten.worktime30.core.BREAK_END
import com.akiwiksten.worktime30.core.BREAK_START
import com.akiwiksten.worktime30.core.DATE
import com.akiwiksten.worktime30.core.END_TIME
import com.akiwiksten.worktime30.core.LUNCH_END
import com.akiwiksten.worktime30.core.LUNCH_START
import com.akiwiksten.worktime30.core.PROJECT_DETAILS_TABLE
import com.akiwiksten.worktime30.core.PROJECT_NAME
import com.akiwiksten.worktime30.core.LUNCH_TIME_ESTIMATE
import com.akiwiksten.worktime30.core.PROJECT_TIME
import com.akiwiksten.worktime30.core.START_TIME
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = PROJECT_DETAILS_TABLE,
    primaryKeys = [DATE, PROJECT_NAME]
)
data class ProjectDetailsEntity(
    @ColumnInfo(name = DATE) val date: String,
    @ColumnInfo(name = PROJECT_NAME) val projectName: String = "",
    @ColumnInfo(name = START_TIME) val startTime: String = "",
    @ColumnInfo(name = END_TIME) val endTime: String = "",
    @ColumnInfo(name = LUNCH_START) val lunchStart: String = "",
    @ColumnInfo(name = LUNCH_END) val lunchEnd: String = "",
    @ColumnInfo(name = BREAK_START) val breakStart: String = "",
    @ColumnInfo(name = BREAK_END) val breakEnd: String = "",
    @ColumnInfo(name = PROJECT_TIME) val projectTime: String = "",
    @ColumnInfo(name = LUNCH_TIME_ESTIMATE) val lunchTimeEstimate: String = "",
)

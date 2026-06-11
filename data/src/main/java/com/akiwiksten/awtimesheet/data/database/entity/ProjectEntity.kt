package com.akiwiksten.awtimesheet.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.akiwiksten.awtimesheet.core.ALLOWANCE
import com.akiwiksten.awtimesheet.core.DATE
import com.akiwiksten.awtimesheet.core.KILOMETRES
import com.akiwiksten.awtimesheet.core.PROJECT_NAME
import com.akiwiksten.awtimesheet.core.PROJECT_TABLE
import com.akiwiksten.awtimesheet.core.PROJECT_TIME
import com.akiwiksten.awtimesheet.core.WORK_TYPE
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = PROJECT_TABLE,
    primaryKeys = [DATE, PROJECT_NAME],
    indices = [Index(value = [PROJECT_NAME])]
)
data class ProjectEntity(
    @ColumnInfo(name = DATE) val date: String,
    @ColumnInfo(name = PROJECT_NAME) val projectName: String = "",
    @ColumnInfo(name = PROJECT_TIME) val projectTime: String = "",
    @ColumnInfo(name = KILOMETRES) val kilometres: Int = 0,
    @ColumnInfo(name = ALLOWANCE) val allowance: String = "",
    @ColumnInfo(name = WORK_TYPE) val workType: String = "",
)

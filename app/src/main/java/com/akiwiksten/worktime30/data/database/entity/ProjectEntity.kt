package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.worktime30.core.ALLOWANCE
import com.akiwiksten.worktime30.core.DATE
import com.akiwiksten.worktime30.core.KILOMETRES
import com.akiwiksten.worktime30.core.PROJECT_END_TIME
import com.akiwiksten.worktime30.core.PROJECT_NAME
import com.akiwiksten.worktime30.core.PROJECT_START_TIME
import com.akiwiksten.worktime30.core.PROJECT_TIME
import com.akiwiksten.worktime30.core.WORK_TYPE
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "project", primaryKeys = [DATE, PROJECT_NAME])
data class ProjectEntity(
    @ColumnInfo(name = DATE) val date: String,
    @ColumnInfo(name = PROJECT_NAME) val projectName: String = "",
    @ColumnInfo(name = PROJECT_START_TIME) val projectStartTime: String = "",
    @ColumnInfo(name = PROJECT_END_TIME) val projectEndTime: String = "",
    @ColumnInfo(name = PROJECT_TIME) val projectTime: String = "",
    @ColumnInfo(name = KILOMETRES) val kilometres: Int = 0,
    @ColumnInfo(name = ALLOWANCE) val allowance: String = "",
    @ColumnInfo(name = WORK_TYPE) val workType: String = "",
)

@Serializable
@Entity(tableName = "projectname")
data class ProjectNameEntity(
    @PrimaryKey val name: String,
)

package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.akiwiksten.worktime30.core.ALLOWANCE
import com.akiwiksten.worktime30.core.DATE
import com.akiwiksten.worktime30.core.KILOMETRES
import com.akiwiksten.worktime30.core.NAME
import com.akiwiksten.worktime30.core.PROJECT_NAME
import com.akiwiksten.worktime30.core.PROJECT_NAME_TABLE
import com.akiwiksten.worktime30.core.PROJECT_TABLE
import com.akiwiksten.worktime30.core.PROJECT_TIME
import com.akiwiksten.worktime30.core.WORK_TYPE
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = PROJECT_TABLE,
    primaryKeys = [DATE, PROJECT_NAME],
    foreignKeys = [
        ForeignKey(
            entity = ProjectNameEntity::class,
            parentColumns = [NAME],
            childColumns = [PROJECT_NAME],
            onDelete = ForeignKey.CASCADE
        )
    ],
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

@Serializable
@Entity(tableName = PROJECT_NAME_TABLE)
data class ProjectNameEntity(
    @PrimaryKey @ColumnInfo(name = NAME) val name: String,
)

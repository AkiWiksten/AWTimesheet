package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.worktime30.core.WORK_TYPE
import com.akiwiksten.worktime30.core.WORK_TYPE_TABLE
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = WORK_TYPE_TABLE)
data class WorkTypeEntity(
    @PrimaryKey @ColumnInfo(name = WORK_TYPE) val workType: String = "",
)

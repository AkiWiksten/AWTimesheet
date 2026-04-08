package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.worktime30.core.EMPLOYER
import com.akiwiksten.worktime30.core.ID
import com.akiwiksten.worktime30.core.NAME
import com.akiwiksten.worktime30.core.SETTINGS_TABLE
import com.akiwiksten.worktime30.core.WORK_TYPE
import com.akiwiksten.worktime30.core.WORK_TYPE_TABLE
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = SETTINGS_TABLE)
data class SettingsEntity(
    @PrimaryKey @ColumnInfo(name = ID) val id: Int = 1,
    @ColumnInfo(name = NAME) val name: String = "",
    @ColumnInfo(name = EMPLOYER) val employer: String = "",
)

@Serializable
@Entity(tableName = WORK_TYPE_TABLE)
data class WorkTypeEntity(
    @PrimaryKey @ColumnInfo(name = WORK_TYPE) val workType: String = "",
)

package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.worktime30.core.EMPLOYER
import com.akiwiksten.worktime30.core.NAME
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = NAME) val name: String = "",
    @ColumnInfo(name = EMPLOYER) val employer: String = "",
)

@Serializable
@Entity(tableName = "worktype")
data class WorkTypeEntity(
    @PrimaryKey val workType: String = "",
)

package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: Int = 1,
    @ColumnInfo(name = "name") val name: String = "",
    @ColumnInfo(name = "employer") val employer: String = "",
)

@Serializable
@Entity(tableName = "work_type")
data class WorkTypeEntity(
    @PrimaryKey @ColumnInfo(name = "work_type") val workType: String = "",
)

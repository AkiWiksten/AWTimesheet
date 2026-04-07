package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "project",
    primaryKeys = ["date", "project_name"],
    foreignKeys = [
        ForeignKey(
            entity = ProjectNameEntity::class,
            parentColumns = ["name"],
            childColumns = ["project_name"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["project_name"])]
)
data class ProjectEntity(
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "project_name") val projectName: String = "",
    @ColumnInfo(name = "duration") val projectTime: String = "",
    @ColumnInfo(name = "kilometres") val kilometres: Int = 0,
    @ColumnInfo(name = "allowance") val allowance: String = "",
    @ColumnInfo(name = "work_type") val workType: String = "",
)

@Serializable
@Entity(tableName = "project_name")
data class ProjectNameEntity(
    @PrimaryKey @ColumnInfo(name = "name") val name: String,
)

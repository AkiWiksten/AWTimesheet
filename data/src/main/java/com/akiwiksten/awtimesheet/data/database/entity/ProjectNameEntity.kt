package com.akiwiksten.awtimesheet.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.awtimesheet.core.NAME
import com.akiwiksten.awtimesheet.core.PROJECT_NAME_TABLE
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = PROJECT_NAME_TABLE)
data class ProjectNameEntity(
    @PrimaryKey @ColumnInfo(name = NAME) val name: String,
)

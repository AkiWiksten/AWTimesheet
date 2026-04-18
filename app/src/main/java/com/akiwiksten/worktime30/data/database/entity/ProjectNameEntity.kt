package com.akiwiksten.worktime30.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.akiwiksten.worktime30.core.NAME
import com.akiwiksten.worktime30.core.PROJECT_NAME_TABLE
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = PROJECT_NAME_TABLE)
data class ProjectNameEntity(
    @PrimaryKey @ColumnInfo(name = NAME) val name: String,
)

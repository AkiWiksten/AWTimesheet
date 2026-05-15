package com.akiwiksten.awtimesheet.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.awtimesheet.data.database.entity.ProjectNameEntity

@Dao
interface ProjectNameDao {
    @Query("SELECT COUNT(*) > 0 FROM project_name")
    suspend fun anyRecords(): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProjectName(project: ProjectNameEntity)

    @Query("SELECT * FROM project_name")
    suspend fun loadProjectNames(): List<ProjectNameEntity>

    @Delete
    suspend fun delete(project: ProjectNameEntity)
}

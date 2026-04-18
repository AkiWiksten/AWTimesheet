package com.akiwiksten.worktime30.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity

@Dao
interface ProjectNameDao {
    @Query("SELECT exists (SELECT 1 FROM project_name)")
    suspend fun anyRecords(): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProjectName(project: ProjectNameEntity)

    @Query("SELECT * FROM project_name")
    suspend fun loadProjectNames(): List<String>

    @Delete
    suspend fun delete(project: ProjectNameEntity)
}

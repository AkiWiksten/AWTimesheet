package com.akiwiksten.worktime30.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.worktime30.core.DATE
import com.akiwiksten.worktime30.core.PROJECT_NAME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity

@Dao
interface ProjectDao {
    @Query("SELECT exists (SELECT 1 FROM project)")
    suspend fun anyRecords(): Boolean

    @Query("SELECT * FROM project")
    suspend fun getAll(): List<ProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("SELECT * FROM project WHERE $DATE = :date")
    suspend fun loadProjectsByDate(date: String): List<ProjectEntity>

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("SELECT * FROM project WHERE $DATE BETWEEN :dateStart AND :dateEnd")
    suspend fun getProjectsByDateRange(dateStart: String, dateEnd: String): List<ProjectEntity>

    @Query("SELECT exists (SELECT 1 FROM project WHERE $PROJECT_NAME = :projectName)")
    suspend fun isProjectNameUsed(projectName: String): Boolean
}

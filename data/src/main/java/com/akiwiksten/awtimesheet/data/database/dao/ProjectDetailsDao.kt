package com.akiwiksten.awtimesheet.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.awtimesheet.core.DATE
import com.akiwiksten.awtimesheet.core.PROJECT_NAME
import com.akiwiksten.awtimesheet.data.database.entity.ProjectDetailsEntity

@Dao
interface ProjectDetailsDao {
    @Query("SELECT COUNT(*) > 0 FROM project_details")
    suspend fun anyRecords(): Boolean

    @Query("SELECT * FROM project_details")
    suspend fun getAll(): List<ProjectDetailsEntity>

    @Query("SELECT * FROM project_details WHERE $DATE = :date AND $PROJECT_NAME = :projectName")
    @Suppress("unused")
    suspend fun loadProjectDetails(date: String, projectName: String): ProjectDetailsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectDetails(projectDetails: ProjectDetailsEntity)

    @Delete
    suspend fun delete(projectDetails: ProjectDetailsEntity)

    @Query("SELECT * FROM project_details WHERE $DATE BETWEEN :dateStart AND :dateEnd")
    @Suppress("unused")
    suspend fun getProjectDetailsByDateRange(dateStart: String, dateEnd: String): List<ProjectDetailsEntity>
}

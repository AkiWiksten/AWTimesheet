package com.akiwiksten.worktime30.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.worktime30.core.DATE
import com.akiwiksten.worktime30.core.PROJECT_NAME
import com.akiwiksten.worktime30.data.database.entity.ProjectDetailsEntity

@Dao
interface ProjectDetailsDao {
    @Query("SELECT exists (SELECT 1 FROM project_details)")
    suspend fun anyRecords(): Boolean

    @Query("SELECT * FROM project_details")
    suspend fun getAll(): List<ProjectDetailsEntity>

    @Query("SELECT * FROM project_details WHERE $DATE = :date AND $PROJECT_NAME = :projectName")
    suspend fun loadProjectDetails(date: String, projectName: String): ProjectDetailsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectDetails(projectDetails: ProjectDetailsEntity)

    @Delete
    suspend fun delete(projectDetails: ProjectDetailsEntity)

    @Query("SELECT * FROM project_details WHERE $DATE BETWEEN :dateStart AND :dateEnd")
    suspend fun getProjectDetailsByDateRange(dateStart: String, dateEnd: String): List<ProjectDetailsEntity>
}

@file: SuppressWarnings("UnusedDeclaration")

package com.akiwiksten.worktime30.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkDayDao {
    @Query("SELECT exists (SELECT 1 FROM workday)")
    suspend fun anyRecords(): Boolean

    @Query("SELECT * FROM workDay")
    suspend fun getAll(): List<WorkDay>

    @Query("SELECT * FROM workDay WHERE date IN (:date)")
    suspend fun loadWorkDay(date: String): WorkDay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkDay(workDay: WorkDay)

    @Delete
    suspend fun delete(workDay: WorkDay)

    @Query("SELECT * FROM workday WHERE date BETWEEN :dateStart AND :dateEnd")
    suspend fun getWorkDaysByDateRange(dateStart: String, dateEnd: String): List<WorkDay>
}

@Dao
interface WorkDayOneRowDao {
    @Query("SELECT * FROM workdayonerow")
    suspend fun loadWorkDayOneRow(): WorkDayOneRow?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkDayOneRow(workDayOneRow: WorkDayOneRow)
}

@Dao
interface ProjectDao {
    @Query("SELECT exists (SELECT 1 FROM project)")
    suspend fun anyRecords(): Boolean

    @Query("SELECT * FROM project")
    suspend fun getAll(): List<Project>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Query("SELECT * FROM project WHERE date IN (:date)")
    suspend fun loadProjectsByDate(date: String): List<Project>

    @Delete
    suspend fun delete(project: Project)

    @Query("SELECT * FROM project WHERE date BETWEEN :dateStart AND :dateEnd")
    suspend fun getProjectsByDateRange(dateStart: String, dateEnd: String): List<Project>
}

@Dao
interface ProjectNameDao {
    @Query("SELECT exists (SELECT 1 FROM projectName)")
    suspend fun anyRecords(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProjectName(project: ProjectName)

    @Query("SELECT * FROM projectName")
    suspend fun loadProjectNames(): List<ProjectName>

    @Delete
    suspend fun delete(project: ProjectName)
}

@Dao
interface SettingsDao {

    @Query("SELECT exists (SELECT 1 FROM settings)")
    suspend fun anyRecord(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: Settings)

    @Query("SELECT * FROM settings")
    suspend fun loadSettings(): Settings?
}

@Dao
interface WorkTypeDao {
    @Query("SELECT exists (SELECT 1 FROM worktype)")
    suspend fun anyRecords(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkType(workType: WorkType)

    @Query("SELECT * FROM worktype")
    suspend fun loadWorkTypes(): List<WorkType>

    @Delete
    suspend fun delete(workType: WorkType)

    @Query("DELETE FROM workType")
    suspend fun deleteAll()
}

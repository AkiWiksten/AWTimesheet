package com.akiwiksten.worktime30.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity

@Dao
interface WorkDayDao {
    @Query("SELECT exists (SELECT 1 FROM workday)")
    suspend fun anyRecords(): Boolean

    @Query("SELECT * FROM workday")
    suspend fun getAll(): List<WorkDayEntity>

    @Query("SELECT * FROM workday WHERE date = :date AND project_name = :projectName")
    suspend fun loadWorkDay(date: String, projectName: String): WorkDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkDay(workDay: WorkDayEntity)

    @Delete
    suspend fun delete(workDay: WorkDayEntity)

    @Query("SELECT * FROM workday WHERE date BETWEEN :dateStart AND :dateEnd")
    suspend fun getWorkDaysByDateRange(dateStart: String, dateEnd: String): List<WorkDayEntity>
}

@Dao
interface WorkStatsDao {
    @Query("SELECT * FROM work_stats WHERE id = 1")
    suspend fun loadWorkStats(): WorkStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkStats(workStats: WorkStatsEntity)
}

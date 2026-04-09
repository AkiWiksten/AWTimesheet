package com.akiwiksten.worktime30.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.worktime30.core.DATE
import com.akiwiksten.worktime30.core.PROJECT_NAME
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity

@Dao
interface WorkdayDao {
    @Query("SELECT exists (SELECT 1 FROM workday)")
    suspend fun anyRecords(): Boolean

    @Query("SELECT * FROM workday")
    suspend fun getAll(): List<WorkdayEntity>

    @Query("SELECT * FROM workday WHERE $DATE = :date AND $PROJECT_NAME = :projectName")
    suspend fun loadWorkday(date: String, projectName: String): WorkdayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkday(workday: WorkdayEntity)

    @Delete
    suspend fun delete(workday: WorkdayEntity)

    @Query("SELECT * FROM workday WHERE $DATE BETWEEN :dateStart AND :dateEnd")
    suspend fun getWorkdaysByDateRange(dateStart: String, dateEnd: String): List<WorkdayEntity>
}

@Dao
interface WorkStatsDao {
    @Query("SELECT * FROM work_stats WHERE id = 1")
    suspend fun loadWorkStats(): WorkStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkStats(workStats: WorkStatsEntity)
}

package com.akiwiksten.worktime30.data.database.dao

import androidx.room.*
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkDayOneRowEntity

@Dao
interface WorkDayDao {
    @Query("SELECT exists (SELECT 1 FROM workday)")
    suspend fun anyRecords(): Boolean

    @Query("SELECT * FROM workday")
    suspend fun getAll(): List<WorkDayEntity>

    @Query("SELECT * FROM workday WHERE date = :date")
    suspend fun loadWorkDay(date: String): WorkDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkDay(workDay: WorkDayEntity)

    @Delete
    suspend fun delete(workDay: WorkDayEntity)

    @Query("SELECT * FROM workday WHERE date BETWEEN :dateStart AND :dateEnd")
    suspend fun getWorkDaysByDateRange(dateStart: String, dateEnd: String): List<WorkDayEntity>
}

@Dao
interface WorkDayOneRowDao {
    @Query("SELECT * FROM workdayonerow WHERE id = 1")
    suspend fun loadWorkDayOneRow(): WorkDayOneRowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkDayOneRow(workDayOneRow: WorkDayOneRowEntity)
}

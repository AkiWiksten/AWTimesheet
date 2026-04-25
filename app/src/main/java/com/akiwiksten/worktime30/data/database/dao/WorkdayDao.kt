package com.akiwiksten.worktime30.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.worktime30.core.DATE
import com.akiwiksten.worktime30.core.WORKDAY_TABLE
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity

@Dao
interface WorkdayDao {
    @Query("SELECT * FROM $WORKDAY_TABLE WHERE $DATE = :date")
    suspend fun loadWorkday(date: String): WorkdayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkday(workday: WorkdayEntity)

    @Query("SELECT * FROM $WORKDAY_TABLE WHERE $DATE BETWEEN :start AND :end ORDER BY $DATE")
    suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayEntity>
}


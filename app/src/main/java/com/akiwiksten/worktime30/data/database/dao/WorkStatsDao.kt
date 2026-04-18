package com.akiwiksten.worktime30.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity

@Dao
interface WorkStatsDao {
    @Query("SELECT * FROM work_stats WHERE id = 1")
    suspend fun loadWorkStats(): WorkStatsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkStats(workStats: WorkStatsEntity)
}

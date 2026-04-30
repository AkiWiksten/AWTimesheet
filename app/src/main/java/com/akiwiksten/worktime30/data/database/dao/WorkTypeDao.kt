package com.akiwiksten.worktime30.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.worktime30.core.WORK_TYPE_TABLE
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity

@Dao
interface WorkTypeDao {
    @Query("SELECT COUNT(*) > 0 FROM $WORK_TYPE_TABLE")
    suspend fun anyRecords(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkType(workType: WorkTypeEntity)

    @Query("SELECT * FROM $WORK_TYPE_TABLE")
    suspend fun loadWorkTypes(): List<WorkTypeEntity>

    @Delete
    suspend fun delete(workType: WorkTypeEntity)

    @Query("DELETE FROM $WORK_TYPE_TABLE")
    suspend fun deleteAllWorkTypes()
}

package com.akiwiksten.worktime30.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity

@Dao
interface SettingsDao {
    @Query("SELECT exists (SELECT 1 FROM settings)")
    suspend fun anyRecord(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)

    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun loadSettings(): SettingsEntity?
}

@Dao
interface WorkTypeDao {
    @Query("SELECT exists (SELECT 1 FROM work_type)")
    suspend fun anyRecords(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkType(workType: WorkTypeEntity)

    @Query("SELECT * FROM work_type")
    suspend fun loadWorkTypes(): List<WorkTypeEntity>

    @Delete
    suspend fun delete(workType: WorkTypeEntity)

    @Query("DELETE FROM work_type")
    suspend fun deleteAll()
}

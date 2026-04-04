package com.akiwiksten.worktime30.data.database.dao

import androidx.room.*
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
    @Query("SELECT exists (SELECT 1 FROM worktype)")
    suspend fun anyRecords(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkType(workType: WorkTypeEntity)

    @Query("SELECT * FROM worktype")
    suspend fun loadWorkTypes(): List<WorkTypeEntity>

    @Delete
    suspend fun delete(workType: WorkTypeEntity)

    @Query("DELETE FROM worktype")
    suspend fun deleteAll()
}

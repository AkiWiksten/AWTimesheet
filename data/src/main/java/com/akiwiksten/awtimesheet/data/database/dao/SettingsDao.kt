package com.akiwiksten.awtimesheet.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.awtimesheet.core.ID
import com.akiwiksten.awtimesheet.core.SETTINGS_TABLE
import com.akiwiksten.awtimesheet.data.database.entity.SettingsEntity

@Dao
interface SettingsDao {
    @Query("SELECT exists (SELECT 1 FROM $SETTINGS_TABLE)")
    suspend fun anyRecord(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: SettingsEntity)

    @Query("SELECT * FROM $SETTINGS_TABLE WHERE $ID = 1")
    suspend fun loadSettings(): SettingsEntity?
}

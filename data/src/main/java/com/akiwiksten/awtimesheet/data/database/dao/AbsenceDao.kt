package com.akiwiksten.awtimesheet.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.awtimesheet.data.database.entity.AbsenceEntity
import com.akiwiksten.awtimesheet.data.database.entity.ProjectEntity

@Dao
interface AbsenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsence(absence: AbsenceEntity)

    @Query("SELECT * FROM absence")
    suspend fun getAll(): List<AbsenceEntity>

    @Delete
    suspend fun delete(absence: AbsenceEntity)
}
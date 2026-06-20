package com.akiwiksten.awtimesheet.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.awtimesheet.data.database.entity.AbsenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AbsenceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAbsence(absence: AbsenceEntity)

    @Query("SELECT * FROM absence")
    fun getAll(): Flow<List<AbsenceEntity>>

    @Delete
    suspend fun delete(absence: AbsenceEntity)
}

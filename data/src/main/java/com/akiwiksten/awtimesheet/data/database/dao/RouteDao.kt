package com.akiwiksten.awtimesheet.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.awtimesheet.data.database.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity)

    @Query("SELECT * FROM route ORDER BY timestamp DESC")
    suspend fun getAll(): List<RouteEntity>

    @Query("SELECT * FROM route ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<RouteEntity>>

    @Query("DELETE FROM route")
    suspend fun clearAll()

    @Delete
    suspend fun delete(absence: RouteEntity)
}
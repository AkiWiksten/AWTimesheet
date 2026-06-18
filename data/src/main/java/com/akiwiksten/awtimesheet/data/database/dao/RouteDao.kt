package com.akiwiksten.awtimesheet.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.awtimesheet.data.database.entity.RouteEntity

@Dao
interface RouteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity)

    @Query("SELECT * FROM route")
    suspend fun getAll(): List<RouteEntity>

    @Delete
    suspend fun delete(absence: RouteEntity)
}
package com.akiwiksten.awtimesheet.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.akiwiksten.awtimesheet.core.CALCULATED_FLEXTIME_TOTAL_TABLE
import com.akiwiksten.awtimesheet.core.ID
import com.akiwiksten.awtimesheet.data.database.entity.CalculatedFlextimeTotalEntity

@Dao
interface CalculatedFlexTimeTotalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculatedFlextimeTotal(flexTime: CalculatedFlextimeTotalEntity)

    @Query("SELECT * FROM $CALCULATED_FLEXTIME_TOTAL_TABLE WHERE $ID = 1")
    suspend fun loadCalculatedFlextimeTotal(): CalculatedFlextimeTotalEntity?
}

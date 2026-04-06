package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity

interface WorkDayRepository {
    suspend fun getWorkDay(date: String, projectName: String = ""): WorkDayEntity?
    suspend fun insertWorkDay(workDay: WorkDayEntity)
    suspend fun deleteWorkDay(workDay: WorkDayEntity)
    suspend fun getWorkStats(): WorkStatsEntity?
    suspend fun insertWorkStats(workStats: WorkStatsEntity)
    suspend fun getWorkDaysByDateRange(start: String, end: String): List<WorkDayEntity>
}

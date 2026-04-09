package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity

interface WorkdayRepository {
    suspend fun getWorkday(date: String, projectName: String = ""): WorkdayEntity?
    suspend fun insertWorkday(workday: WorkdayEntity)
    suspend fun deleteWorkday(workday: WorkdayEntity)
    suspend fun getWorkStats(): WorkStatsEntity?
    suspend fun insertWorkStats(workStats: WorkStatsEntity)
    suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayEntity>
}

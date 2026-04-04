package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkDayOneRowEntity

interface WorkDayRepository {
    suspend fun getWorkDay(date: String): WorkDayEntity?
    suspend fun insertWorkDay(workDay: WorkDayEntity)
    suspend fun getWorkDayOneRow(): WorkDayOneRowEntity?
    suspend fun insertWorkDayOneRow(workDayOneRow: WorkDayOneRowEntity)
    suspend fun getWorkDaysByDateRange(start: String, end: String): List<WorkDayEntity>
}

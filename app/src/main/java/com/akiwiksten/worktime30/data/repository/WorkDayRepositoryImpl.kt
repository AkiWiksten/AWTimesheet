package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.WorkDayDao
import com.akiwiksten.worktime30.data.database.dao.WorkDayOneRowDao
import com.akiwiksten.worktime30.data.database.entity.WorkDayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkDayOneRowEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkDayRepositoryImpl @Inject constructor(
    private val workDayDao: WorkDayDao,
    private val workDayOneRowDao: WorkDayOneRowDao
) : WorkDayRepository {
    override suspend fun getWorkDay(date: String): WorkDayEntity? = workDayDao.loadWorkDay(date)
    override suspend fun insertWorkDay(workDay: WorkDayEntity) = workDayDao.insertWorkDay(workDay)
    override suspend fun getWorkDayOneRow(): WorkDayOneRowEntity? = workDayOneRowDao.loadWorkDayOneRow()
    override suspend fun insertWorkDayOneRow(workDayOneRow: WorkDayOneRowEntity) =
        workDayOneRowDao.insertWorkDayOneRow(workDayOneRow)
}

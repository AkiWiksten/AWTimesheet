package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.WorkStatsDao
import com.akiwiksten.worktime30.data.database.dao.WorkdayDao
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkdayRepositoryImplTest {
    private val workdayDao = FakeWorkdayDao()
    private val workStatsDao = FakeWorkStatsDao()
    private val repository = WorkdayRepositoryImpl(workdayDao, workStatsDao)

    @Test
    fun getWorkday_returnsDataFromDao() = runBlocking {
        val expected = WorkdayEntity(date = "2026-04-10", projectName = "Alpha")
        workdayDao.workdayResult = expected

        val result = repository.getWorkday("2026-04-10", "Alpha")

        assertEquals(expected, result)
        assertEquals("2026-04-10", workdayDao.lastDate)
        assertEquals("Alpha", workdayDao.lastProjectName)
    }

    @Test
    fun insertWorkday_callsDaoInsert() = runBlocking {
        val workday = WorkdayEntity(date = "2026-04-10", projectName = "Alpha")

        repository.insertWorkday(workday)

        assertEquals(workday, workdayDao.insertedWorkday)
    }

    @Test
    fun deleteWorkday_callsDaoDelete() = runBlocking {
        val workday = WorkdayEntity(date = "2026-04-10", projectName = "Alpha")

        repository.deleteWorkday(workday)

        assertEquals(workday, workdayDao.deletedWorkday)
    }

    @Test
    fun getWorkStats_returnsDataFromDao() = runBlocking {
        val expected = WorkStatsEntity(workTimeTotal = "10:00 h")
        workStatsDao.workStatsResult = expected

        val result = repository.getWorkStats()

        assertEquals(expected, result)
    }

    @Test
    fun insertWorkStats_callsDaoInsert() = runBlocking {
        val workStats = WorkStatsEntity(workTimeTotal = "10:00 h")

        repository.insertWorkStats(workStats)

        assertEquals(workStats, workStatsDao.insertedWorkStats)
    }

    @Test
    fun getWorkdaysByDateRange_returnsDataFromDao() = runBlocking {
        val expected = listOf(WorkdayEntity(date = "2026-04-10", projectName = "Alpha"))
        workdayDao.workdaysByDateRangeResult = expected

        val result = repository.getWorkdaysByDateRange("2026-04-01", "2026-04-30")

        assertEquals(expected, result)
        assertEquals("2026-04-01", workdayDao.lastDateStart)
        assertEquals("2026-04-30", workdayDao.lastDateEnd)
    }

    private class FakeWorkdayDao : WorkdayDao {
        var workdayResult: WorkdayEntity? = null
        var workdaysByDateRangeResult: List<WorkdayEntity> = emptyList()
        var insertedWorkday: WorkdayEntity? = null
        var deletedWorkday: WorkdayEntity? = null
        var lastDate: String? = null
        var lastProjectName: String? = null
        var lastDateStart: String? = null
        var lastDateEnd: String? = null

        override suspend fun anyRecords(): Boolean = false

        override suspend fun getAll(): List<WorkdayEntity> = emptyList()

        override suspend fun loadWorkday(date: String, projectName: String): WorkdayEntity? {
            lastDate = date
            lastProjectName = projectName
            return workdayResult
        }

        override suspend fun insertWorkday(workday: WorkdayEntity) {
            insertedWorkday = workday
        }

        override suspend fun delete(workday: WorkdayEntity) {
            deletedWorkday = workday
        }

        override suspend fun getWorkdaysByDateRange(dateStart: String, dateEnd: String): List<WorkdayEntity> {
            lastDateStart = dateStart
            lastDateEnd = dateEnd
            return workdaysByDateRangeResult
        }
    }

    private class FakeWorkStatsDao : WorkStatsDao {
        var workStatsResult: WorkStatsEntity? = null
        var insertedWorkStats: WorkStatsEntity? = null

        override suspend fun loadWorkStats(): WorkStatsEntity? = workStatsResult

        override suspend fun insertWorkStats(workStats: WorkStatsEntity) {
            insertedWorkStats = workStats
        }
    }
}


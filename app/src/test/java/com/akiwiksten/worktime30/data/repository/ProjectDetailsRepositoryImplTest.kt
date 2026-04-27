@file:Suppress("ImportOrdering")

package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.ProjectDetailsDao
import com.akiwiksten.worktime30.data.database.dao.WorkdayDao
import com.akiwiksten.worktime30.data.database.dao.WorkStatsDao
import com.akiwiksten.worktime30.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkdayEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.WorkStatsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectDetailsRepositoryImplTest {
    private val projectDetailsDao = FakeProjectDetailsDao()
    private val workStatsDao = FakeWorkStatsDao()
    private val workdayDao = FakeWorkdayDao()
    private val repository = ProjectDetailsRepositoryImpl(projectDetailsDao, workStatsDao, workdayDao)

    @Test
    fun getProjectDetails_returnsDataFromDao() = runBlocking {
        val expected = ProjectDetailsState(date = "2026-04-10", projectName = "Alpha")
        projectDetailsDao.projectDetailsResult = expected.toEntity()

        val result = repository.getProjectDetails("2026-04-10", "Alpha")

        assertEquals(expected, result)
        assertEquals("2026-04-10", projectDetailsDao.lastDate)
        assertEquals("Alpha", projectDetailsDao.lastProjectName)
    }

    @Test
    fun insertProjectDetails_callsDaoInsert() = runBlocking {
        val projectDetails = ProjectDetailsState(date = "2026-04-10", projectName = "Alpha")

        repository.insertProjectDetails(projectDetails)

        assertEquals(projectDetails.toEntity(), projectDetailsDao.insertedProjectDetails)
    }

    @Test
    fun deleteProjectDetails_callsDaoDelete() = runBlocking {
        val projectDetails = ProjectDetailsState(date = "2026-04-10", projectName = "Alpha")

        repository.deleteProjectDetails(projectDetails)

        assertEquals(projectDetails.toEntity(), projectDetailsDao.deletedProjectDetails)
    }

    @Test
    fun getWorkStats_returnsDataFromDao() = runBlocking {
        val expected = WorkStatsState(initialFlexTimeTotal = "10:00 h")
        workStatsDao.workStatsResult = expected.toEntity()

        val result = repository.getWorkStats()

        assertEquals(expected, result)
    }

    @Test
    fun insertWorkStats_callsDaoInsert() = runBlocking {
        val workStats = WorkStatsState(initialFlexTimeTotal = "10:00 h")

        repository.insertWorkStats(workStats)

        assertEquals(workStats.toEntity(), workStatsDao.insertedWorkStats)
    }

    @Test
    fun getWorkStatsByDate_returnsWorkdayDataWhenAvailable() = runBlocking {
        workStatsDao.workStatsResult = WorkStatsState(
            dailyWorkTimeEstimate = "07:30",
            dailyLunchTimeEstimate = "00:30",
            initialFlexTimeTotal = "+01:00"
        ).toEntity()
        workdayDao.workdayResult = WorkdayEntity(
            date = "2026-04-10",
            workTimeToday = "00:00",
            workTimeTodayEstimate = "08:00"
        )

        val result = repository.getWorkStatsByDate("2026-04-10")

        assertEquals("08:00", result?.dailyWorkTimeEstimate)
        assertEquals("00:30", result?.dailyLunchTimeEstimate)
        assertEquals("+01:00", result?.initialFlexTimeTotal)
    }

    @Test
    fun upsertWorkdayStats_callsWorkdayDaoInsert() = runBlocking {
        val workStats = WorkStatsState(
            dailyWorkTimeEstimate = "08:00",
            dailyLunchTimeEstimate = "00:30",
            initialFlexTimeTotal = "-00:20"
        )

        repository.upsertWorkdayStats(date = "2026-04-10", workTimeToday = "00:00", workStats = workStats)

        assertEquals("2026-04-10", workdayDao.insertedWorkday?.date)
        assertEquals("00:00", workdayDao.insertedWorkday?.workTimeToday)
        assertEquals("08:00", workdayDao.insertedWorkday?.workTimeTodayEstimate)
        assertEquals("08:00", workStatsDao.insertedWorkStats?.dailyWorkTimeEstimate)
        assertEquals("00:30", workStatsDao.insertedWorkStats?.dailyLunchTimeEstimate)
        assertEquals("-00:20", workStatsDao.insertedWorkStats?.initialFlexTimeTotal)
    }

    @Test
    fun getProjectDetailsByDateRange_returnsDataFromDao() = runBlocking {
        val expected = listOf(ProjectDetailsState(date = "2026-04-10", projectName = "Alpha"))
        projectDetailsDao.projectDetailsByDateRangeResult = expected.map { it.toEntity() }

        val result = repository.getProjectDetailsByDateRange("2026-04-01", "2026-04-30")

        assertEquals(expected, result)
        assertEquals("2026-04-01", projectDetailsDao.lastDateStart)
        assertEquals("2026-04-30", projectDetailsDao.lastDateEnd)
    }

    @Test
    fun getWorkdayStatsByDateRange_returnsRowsFromWorkdayDao() = runBlocking {
        workdayDao.workdaysByDateRange = listOf(
            WorkdayEntity(date = "2026-04-10", workTimeToday = "08:00", workTimeTodayEstimate = "07:30"),
            WorkdayEntity(date = "2026-04-11", workTimeToday = "07:00", workTimeTodayEstimate = "07:30")
        )

        val result = repository.getWorkdayStatsByDateRange("2026-04-01", "2026-04-30")

        assertEquals(2, result.size)
        assertEquals("2026-04-10", result[0].date)
        assertEquals("08:00", result[0].workTimeToday)
        assertEquals("07:30", result[0].workTimeTodayEstimate)
    }

    private class FakeProjectDetailsDao : ProjectDetailsDao {
        var projectDetailsResult: ProjectDetailsEntity? = null
        var projectDetailsByDateRangeResult: List<ProjectDetailsEntity> = emptyList()
        var insertedProjectDetails: ProjectDetailsEntity? = null
        var deletedProjectDetails: ProjectDetailsEntity? = null
        var lastDate: String? = null
        var lastProjectName: String? = null
        var lastDateStart: String? = null
        var lastDateEnd: String? = null

        override suspend fun anyRecords(): Boolean = false

        override suspend fun getAll(): List<ProjectDetailsEntity> = emptyList()

        override suspend fun loadProjectDetails(date: String, projectName: String): ProjectDetailsEntity? {
            lastDate = date
            lastProjectName = projectName
            return projectDetailsResult
        }

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsEntity) {
            insertedProjectDetails = projectDetails
        }

        override suspend fun delete(projectDetails: ProjectDetailsEntity) {
            deletedProjectDetails = projectDetails
        }

        override suspend fun getProjectDetailsByDateRange(
            dateStart: String,
            dateEnd: String
        ): List<ProjectDetailsEntity> {
            lastDateStart = dateStart
            lastDateEnd = dateEnd
            return projectDetailsByDateRangeResult
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

    private class FakeWorkdayDao : WorkdayDao {
        var workdayResult: WorkdayEntity? = null
        var insertedWorkday: WorkdayEntity? = null
        var workdaysByDateRange: List<WorkdayEntity> = emptyList()

        override suspend fun loadWorkday(date: String): WorkdayEntity? = workdayResult

        override suspend fun insertWorkday(workday: WorkdayEntity) {
            insertedWorkday = workday
        }

        override suspend fun getWorkdaysByDateRange(
            start: String,
            end: String
        ): List<WorkdayEntity> = workdaysByDateRange
    }
}

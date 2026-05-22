@file:Suppress("ImportOrdering")

package com.akiwiksten.awtimesheet.data.repository

import com.akiwiksten.awtimesheet.data.database.mapper.toEntity
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.repository.impl.ProjectDetailsRepositoryImpl
import com.akiwiksten.awtimesheet.test.FakeProjectDetailsDao
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectDetailsRepositoryImplTest {
    private val projectDetailsDao = FakeProjectDetailsDao()
    private val repository = ProjectDetailsRepositoryImpl(projectDetailsDao)

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
    fun getProjectDetailsByDateRange_returnsDataFromDao() = runBlocking {
        val expected: List<ProjectDetailsState> = listOf(
            ProjectDetailsState(date = "2026-04-10", projectName = "Alpha")
        )
        projectDetailsDao.projectDetailsByDateRangeResult = listOf(expected.first().toEntity())

        val result = repository.getProjectDetailsByDateRange("2026-04-01", "2026-04-30")

        assertEquals(expected, result)
        assertEquals("2026-04-01", projectDetailsDao.lastDateStart)
        assertEquals("2026-04-30", projectDetailsDao.lastDateEnd)
    }
}

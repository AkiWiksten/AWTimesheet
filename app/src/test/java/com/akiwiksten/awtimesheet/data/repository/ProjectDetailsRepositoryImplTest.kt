@file:Suppress("ImportOrdering")

package com.akiwiksten.awtimesheet.data.repository

import com.akiwiksten.awtimesheet.data.database.dao.ProjectDetailsDao
import com.akiwiksten.awtimesheet.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.awtimesheet.data.database.mapper.toEntity
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
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
        val expected = listOf(ProjectDetailsState(date = "2026-04-10", projectName = "Alpha"))
        projectDetailsDao.projectDetailsByDateRangeResult = expected.map { it.toEntity() }

        val result = repository.getProjectDetailsByDateRange("2026-04-01", "2026-04-30")

        assertEquals(expected, result)
        assertEquals("2026-04-01", projectDetailsDao.lastDateStart)
        assertEquals("2026-04-30", projectDetailsDao.lastDateEnd)
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
}

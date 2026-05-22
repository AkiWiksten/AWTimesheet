package com.akiwiksten.awtimesheet.data.repository

import com.akiwiksten.awtimesheet.data.database.entity.ProjectEntity
import com.akiwiksten.awtimesheet.data.database.mapper.toDomain
import com.akiwiksten.awtimesheet.data.database.mapper.toEntity
import com.akiwiksten.awtimesheet.domain.repository.impl.ProjectRepositoryImpl
import com.akiwiksten.awtimesheet.test.FakeProjectDao
import com.akiwiksten.awtimesheet.test.FakeProjectNameDao
import com.akiwiksten.awtimesheet.test.projectState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectRepositoryImplTest {
    private val projectDao = FakeProjectDao()
    private val projectNameDao = FakeProjectNameDao()
    private val repository = ProjectRepositoryImpl(projectDao, projectNameDao)

    @Test
    fun getProjectsByDateRange_returnsDataFromDao() = runBlocking {
        val expected = listOf(ProjectEntity(date = "2026-04-10", projectName = "Alpha"))
        projectDao.projectsByDateRangeResult = expected

        val result = repository.getProjectsByDateRange("2026-04-01", "2026-04-30")

        val expectedDomain = expected.map { it.toDomain() }
        assertEquals(expectedDomain, result)
        assertEquals("2026-04-01", projectDao.lastDateStart)
        assertEquals("2026-04-30", projectDao.lastDateEnd)
    }

    @Test
    fun insertProject_callsDaoInsert() = runBlocking {
        val project = projectState(date = "2026-04-10", projectName = "Alpha")

        repository.insertProject(project)

        assertEquals(project.toEntity(), projectDao.insertedProject)
    }

    @Test
    fun deleteProject_callsDaoDelete() = runBlocking {
        val project = projectState(date = "2026-04-10", projectName = "Alpha")

        repository.deleteProject(project)

        assertEquals(project.toEntity(), projectDao.deletedProject)
    }

    @Test
    fun getProjectNames_returnsDataFromDao() = runBlocking {
        val expected = listOf("Alpha", "Beta")
        projectNameDao.projectNamesResult = expected

        val result = repository.getProjectNames()

        assertEquals(expected, result)
    }

    @Test
    fun insertProjectName_callsDaoInsert() = runBlocking {
        val projectName = "Alpha"

        repository.insertProjectName(projectName)

        assertEquals(projectName, projectNameDao.insertedProjectName)
    }

    @Test
    fun deleteProjectName_callsDaoDelete() = runBlocking {
        val projectName = "Alpha"

        repository.deleteProjectName(projectName)

        assertEquals(projectName, projectNameDao.deletedProjectName)
    }

    @Test
    fun isProjectNameUsed_returnsValueFromDao() = runBlocking {
        projectDao.projectNameUsed = true

        val used = repository.isProjectNameUsed("Alpha")

        assertTrue(used)

        projectDao.projectNameUsed = false
        val notUsed = repository.isProjectNameUsed("Beta")

        assertFalse(notUsed)
    }
}

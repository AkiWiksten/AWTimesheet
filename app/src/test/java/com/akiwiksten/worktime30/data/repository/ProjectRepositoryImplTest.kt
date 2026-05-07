package com.akiwiksten.worktime30.data.repository

import com.akiwiksten.worktime30.data.database.dao.ProjectDao
import com.akiwiksten.worktime30.data.database.dao.ProjectNameDao
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.mapper.toDomain
import com.akiwiksten.worktime30.data.database.mapper.toEntity
import com.akiwiksten.worktime30.data.database.mapper.toProjectNameEntity
import com.akiwiksten.worktime30.domain.model.SingleProjectState
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
        val project = SingleProjectState(date = "2026-04-10", projectName = "Alpha")

        repository.insertProject(project)

        assertEquals(project.toEntity(), projectDao.insertedProject)
    }

    @Test
    fun deleteProject_callsDaoDelete() = runBlocking {
        val project = SingleProjectState(date = "2026-04-10", projectName = "Alpha")

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

    private class FakeProjectDao : ProjectDao {
        var projectsByDateRangeResult: List<ProjectEntity> = emptyList()
        var insertedProject: ProjectEntity? = null
        var deletedProject: ProjectEntity? = null
        var lastDateStart: String? = null
        var lastDateEnd: String? = null
        var projectNameUsed: Boolean = false

        override suspend fun anyRecords(): Boolean = false

        override suspend fun getAll(): List<ProjectEntity> = emptyList()

        override suspend fun insertProject(project: ProjectEntity) {
            insertedProject = project
        }

        override suspend fun loadProjectsByDate(date: String): List<ProjectEntity> = emptyList()
        override suspend fun loadProject(
            date: String,
            projectName: String
        ): ProjectEntity? = null

        override suspend fun delete(project: ProjectEntity) {
            deletedProject = project
        }

        override suspend fun getProjectsByDateRange(dateStart: String, dateEnd: String): List<ProjectEntity> {
            lastDateStart = dateStart
            lastDateEnd = dateEnd
            return projectsByDateRangeResult
        }

        override suspend fun isProjectNameUsed(projectName: String): Boolean = projectNameUsed

        override suspend fun getProjectTimesByDate(date: String): List<String> = emptyList()
    }

    private class FakeProjectNameDao : ProjectNameDao {
        var projectNamesResult: List<String> = emptyList()
        var insertedProjectName: String? = null
        var deletedProjectName: String? = null

        override suspend fun anyRecords(): Boolean = false

        override suspend fun insertProjectName(project: ProjectNameEntity) {
            insertedProjectName = project.name
        }

        override suspend fun loadProjectNames(): List<ProjectNameEntity> =
            projectNamesResult.map { it.toProjectNameEntity() }

        override suspend fun delete(project: ProjectNameEntity) {
            deletedProjectName = project.name
        }
    }
}

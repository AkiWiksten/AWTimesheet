package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetProjectsByMonthUseCaseTest {

    @Test
    fun invoke_requestsFullMonthRange_forRegularMonth() = runBlocking {
        val repository = FakeProjectRepository().apply {
            projectsResult = listOf(ProjectEntity(date = "2026-04-10", projectName = "Alpha"))
        }
        val useCase = GetProjectsByMonthUseCase(repository)

        val result = useCase("2026-04-10")

        assertEquals("2026-04-01", repository.lastStart)
        assertEquals("2026-04-30", repository.lastEnd)
        assertEquals(1, result.size)
    }

    @Test
    fun invoke_requestsFullMonthRange_forLeapYearFebruary() = runBlocking {
        val repository = FakeProjectRepository()
        val useCase = GetProjectsByMonthUseCase(repository)

        useCase("2024-02-15")

        assertEquals("2024-02-01", repository.lastStart)
        assertEquals("2024-02-29", repository.lastEnd)
    }

    private class FakeProjectRepository : ProjectRepository {
        var lastStart: String? = null
        var lastEnd: String? = null
        var projectsResult: List<ProjectEntity> = emptyList()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<ProjectEntity> {
            lastStart = start
            lastEnd = end
            return projectsResult
        }

        override suspend fun insertProject(project: ProjectEntity) = Unit

        override suspend fun deleteProject(project: ProjectEntity) = Unit

        override suspend fun getProjectNames(): List<ProjectNameEntity> = emptyList()

        override suspend fun insertProjectName(projectName: ProjectNameEntity) = Unit

        override suspend fun deleteProjectName(projectName: ProjectNameEntity) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean = false
    }
}

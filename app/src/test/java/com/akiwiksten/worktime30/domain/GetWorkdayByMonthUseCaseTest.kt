package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.usecase.GetWorkdayByMonthUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetWorkdayByMonthUseCaseTest {

    @Test
    fun invoke_requestsFullMonthRange_forRegularMonth() = runBlocking {
        val repository = FakeProjectRepository().apply {
            projectsResult = listOf(SingleProjectState(date = "2026-04-10", projectName = "Alpha"))
        }
        val useCase = GetWorkdayByMonthUseCase(repository)

        val result = useCase("2026-04-10")

        assertEquals("2026-04-01", repository.lastStart)
        assertEquals("2026-04-30", repository.lastEnd)
        assertEquals(1, result.size)
    }

    @Test
    fun invoke_requestsFullMonthRange_forLeapYearFebruary() = runBlocking {
        val repository = FakeProjectRepository()
        val useCase = GetWorkdayByMonthUseCase(repository)

        useCase("2024-02-15")

        assertEquals("2024-02-01", repository.lastStart)
        assertEquals("2024-02-29", repository.lastEnd)
    }

    private class FakeProjectRepository : ProjectRepository {
        var lastStart: String? = null
        var lastEnd: String? = null
        var projectsResult: List<SingleProjectState> = emptyList()

        override suspend fun anyRecords(): Boolean = false

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> {
            lastStart = start
            lastEnd = end
            return projectsResult
        }

        override suspend fun insertProject(project: SingleProjectState) = Unit

        override suspend fun deleteProject(project: SingleProjectState) = Unit

        override suspend fun getProjectNames(): List<String> = emptyList()

        override suspend fun insertProjectName(projectName: String) = Unit

        override suspend fun deleteProjectName(projectName: String) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean = false

        override suspend fun getProjectTimeSumByDate(date: String): String = ZERO_TIME
    }
}

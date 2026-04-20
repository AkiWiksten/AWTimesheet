package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetCalendarDataUseCaseTest {

    @Test
    fun invoke_usesMonthlyDataForWeek_whenWeekInsideMonth() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projectsByRange["2026-04-01|2026-04-30"] = listOf(
                SingleProjectState(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00"),
                SingleProjectState(date = "2026-04-15", projectName = "Alpha", projectTime = "05:00"),
                SingleProjectState(date = "2026-04-16", projectName = "Beta", projectTime = "03:00")
            )
        }
        val useCase = GetCalendarDataUseCase(projectRepository)

        val result = useCase("2026-04-15")

        assertEquals("16:00", result.timePerMonth)
        assertEquals("08:00", result.timePerWeek)
        assertEquals("05:00", result.timePerDay)
        assertEquals(1, projectRepository.requestedRanges.size)
    }

    @Test
    fun invoke_fetchesWeekSeparately_whenWeekOverlapsMonthBoundary() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projectsByRange["2026-08-01|2026-08-31"] = emptyList()
            projectsByRange["2026-07-27|2026-08-02"] = listOf(
                SingleProjectState(date = "2026-07-30", projectName = "Alpha", projectTime = "02:00")
            )
        }
        val useCase = GetCalendarDataUseCase(projectRepository)

        val result = useCase("2026-08-01")

        assertEquals("00:00", result.timePerMonth)
        assertEquals("02:00", result.timePerWeek)
        assertEquals("00:00", result.timePerDay)
        assertEquals(listOf("2026-08-01|2026-08-31", "2026-07-27|2026-08-02"), projectRepository.requestedRanges)
    }

    private class FakeProjectRepository : ProjectRepository {
        val projectsByRange = mutableMapOf<String, List<SingleProjectState>>()
        val requestedRanges = mutableListOf<String>()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> {
            val key = "$start|$end"
            requestedRanges += key
            return projectsByRange[key] ?: emptyList()
        }

        override suspend fun insertProject(project: SingleProjectState) = Unit

        override suspend fun deleteProject(project: SingleProjectState) = Unit

        override suspend fun getProjectNames(): List<String> = emptyList()

        override suspend fun insertProjectName(projectName: String) = Unit

        override suspend fun deleteProjectName(projectName: String) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean = false
    }
}

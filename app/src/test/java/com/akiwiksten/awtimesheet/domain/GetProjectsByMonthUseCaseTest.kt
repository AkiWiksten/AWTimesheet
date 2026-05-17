package com.akiwiksten.awtimesheet.domain

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayStatsRow
import com.akiwiksten.awtimesheet.domain.usecase.GetProjectsByMonthUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetProjectsByMonthUseCaseTest {

    @Test
    fun invoke_requestsFullMonthRange_forRegularMonth() = runBlocking {
        val repository = FakeProjectRepository().apply {
            projectsResult = listOf(SingleProjectState(date = "2026-04-10", projectName = "Alpha"))
        }
        val useCase = GetProjectsByMonthUseCase(
            projectRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            workdayRepository = FakeWorkdayRepository()
        )

        val result = useCase("2026-04-10")

        assertEquals("2026-04-01", repository.lastStart)
        assertEquals("2026-04-30", repository.lastEnd)
        assertEquals("2026-04-30", result.endOfMonth)
        assertEquals(1, result.projects.size)
    }

    @Test
    fun invoke_requestsFullMonthRange_forLeapYearFebruary() = runBlocking {
        val repository = FakeProjectRepository()
        val useCase = GetProjectsByMonthUseCase(
            projectRepository = repository,
            settingsRepository = FakeSettingsRepository(),
            workdayRepository = FakeWorkdayRepository()
        )

        val result = useCase("2024-02-15")

        assertEquals("2024-02-01", repository.lastStart)
        assertEquals("2024-02-29", repository.lastEnd)
        assertEquals("2024-02-29", result.endOfMonth)
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

        override suspend fun getProject(date: String, projectName: String): SingleProjectState? = null

        override suspend fun getWorkTimeByDate(date: String): String = ZERO_TIME
    }

    private class FakeSettingsRepository : SettingsRepository {
        override suspend fun getSettings(): SettingsState? = null

        override suspend fun insertSettings(settings: SettingsState) = Unit

        override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? = null

        override suspend fun getWorkTypes(): List<String> = emptyList()

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun deleteAllWorkTypes() = Unit
    }

    private class FakeWorkdayRepository : WorkdayRepository {
        override suspend fun loadWorkday(date: String): String? = null

        override suspend fun upsertWorkdayStats(date: String, workTimeByDateEstimate: String) = Unit

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> = emptyList()
    }
}

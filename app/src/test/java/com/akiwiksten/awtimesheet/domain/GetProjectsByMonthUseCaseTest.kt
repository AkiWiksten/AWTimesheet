package com.akiwiksten.awtimesheet.domain

import com.akiwiksten.awtimesheet.domain.usecase.GetProjectsByMonthUseCase
import com.akiwiksten.awtimesheet.test.FakeProjectRepository
import com.akiwiksten.awtimesheet.test.FakeSettingsRepository
import com.akiwiksten.awtimesheet.test.projectState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetProjectsByMonthUseCaseTest {

    @Test
    fun invoke_requestsFullMonthRange_forRegularMonth() = runBlocking {
        val repository = FakeProjectRepository().apply {
            projectsResult = listOf(projectState(date = "2026-04-10", projectName = "Alpha"))
        }
        val useCase = GetProjectsByMonthUseCase(
            projectRepository = repository,
            settingsRepository = FakeSettingsRepository()
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
            settingsRepository = FakeSettingsRepository()
        )

        val result = useCase("2024-02-15")

        assertEquals("2024-02-01", repository.lastStart)
        assertEquals("2024-02-29", repository.lastEnd)
        assertEquals("2024-02-29", result.endOfMonth)
    }
}

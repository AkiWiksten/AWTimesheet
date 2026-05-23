package com.akiwiksten.awtimesheet.domain

import com.akiwiksten.awtimesheet.domain.usecase.GetCalendarDataUseCase
import com.akiwiksten.awtimesheet.test.FakeProjectRepository
import com.akiwiksten.awtimesheet.test.projectState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetCalendarDataUseCaseTest {

    @Test
    fun invoke_usesMonthlyDataForWeek_whenWeekInsideMonth() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projectsByRange["2026-04-01|2026-04-30"] = listOf(
                projectState(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00"),
                projectState(date = "2026-04-15", projectName = "Alpha", projectTime = "05:00"),
                projectState(date = "2026-04-16", projectName = "Beta", projectTime = "03:00")
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
                projectState(date = "2026-07-30", projectName = "Alpha", projectTime = "02:00")
            )
        }
        val useCase = GetCalendarDataUseCase(projectRepository)

        val result = useCase("2026-08-01")

        assertEquals("00:00", result.timePerMonth)
        assertEquals("02:00", result.timePerWeek)
        assertEquals("00:00", result.timePerDay)
        assertEquals(listOf("2026-08-01|2026-08-31", "2026-07-27|2026-08-02"), projectRepository.requestedRanges)
    }
}

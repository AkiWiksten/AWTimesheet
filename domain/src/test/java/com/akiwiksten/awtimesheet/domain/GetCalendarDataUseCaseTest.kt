package com.akiwiksten.awtimesheet.domain

import com.akiwiksten.awtimesheet.domain.usecase.GetCalendarDataUseCase
import com.akiwiksten.awtimesheet.test.FakeProjectRepository
import com.akiwiksten.awtimesheet.test.projectState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetCalendarDataUseCaseTest {

    @Test
    fun invoke_alwaysFetchesWeekAndDaySeparately() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projectsResult = listOf(
                projectState(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00"),
                projectState(date = "2026-04-15", projectName = "Alpha", projectTime = "05:00"),
                projectState(date = "2026-04-16", projectName = "Beta", projectTime = "03:00")
            )
        }
        val useCase = GetCalendarDataUseCase(projectRepository)

        val result = useCase("2026-04-15")

        assertEquals("16:00", result.timePerMonth)
        assertEquals("08:00", result.timePerWeek) // Apr 15 (05:00) + Apr 16 (03:00) in same week
        assertEquals("05:00", result.timePerDay)
        // First call: month + week + day = 3 range queries
        assertEquals(3, projectRepository.requestedRanges.size)
    }

    @Test
    fun invoke_fetchesWeekSeparately_whenWeekOverlapsMonthBoundary() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projectsResult = listOf(
                projectState(date = "2026-07-30", projectName = "Alpha", projectTime = "02:00")
            )
        }
        val useCase = GetCalendarDataUseCase(projectRepository)

        val result = useCase("2026-08-01")

        assertEquals("00:00", result.timePerMonth)
        assertEquals("02:00", result.timePerWeek)
        assertEquals("00:00", result.timePerDay)
        // month + week + day = 3 range queries
        assertEquals(3, projectRepository.requestedRanges.size)
        assertEquals("2026-08-01|2026-08-31", projectRepository.requestedRanges[0])
        assertEquals("2026-08-01|2026-08-01", projectRepository.requestedRanges[2])
    }

    @Test
    fun invoke_sameMonth_appliesChangeToCachedMonthlyTotal() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projectsResult = listOf(
                projectState(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00")
            )
        }
        val useCase = GetCalendarDataUseCase(projectRepository)

        val initial = useCase("2026-04-10")
        val updated = useCase(date = "2026-04-10", workTimeByDateChange = "02:30")
        val rereadWithoutChange = useCase("2026-04-10")

        assertEquals("08:00", initial.timePerMonth)
        assertEquals("10:30", updated.timePerMonth)
        assertEquals("10:30", rereadWithoutChange.timePerMonth)
    }

    @Test
    fun invoke_sameMonth_skipsMonthFetch_whenCached() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projectsResult = listOf(
                projectState(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00")
            )
        }
        val useCase = GetCalendarDataUseCase(projectRepository)

        useCase("2026-04-10") // 1st call: month + week + day
        useCase(date = "2026-04-10", workTimeByDateChange = "02:30") // 2nd call: week + day only
        useCase("2026-04-10") // 3rd call: week + day only

        // 1 month fetch + 3×week + 3×day = 7 total range queries
        assertEquals(7, projectRepository.requestedRanges.size)
        val monthRangeCount = projectRepository.requestedRanges.count { it == "2026-04-01|2026-04-30" }
        assertEquals(1, monthRangeCount)
    }

    @Test
    fun invoke_monthChanged_recalculatesMonthlyTotalFromScratch() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projectsResult = listOf(
                projectState(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00"),
                projectState(date = "2026-05-02", projectName = "Beta", projectTime = "03:00")
            )
        }
        val useCase = GetCalendarDataUseCase(projectRepository)

        useCase(date = "2026-04-10", workTimeByDateChange = "02:30")
        val mayResult = useCase("2026-05-02")

        assertEquals("03:00", mayResult.timePerMonth)
    }
}

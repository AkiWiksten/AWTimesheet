package com.akiwiksten.awtimesheet.domain

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.usecase.GetWorkdayScreenDataUseCase
import com.akiwiksten.awtimesheet.test.FakeProjectRepository
import com.akiwiksten.awtimesheet.test.FakeSettingsRepository
import com.akiwiksten.awtimesheet.test.projectState
import com.akiwiksten.awtimesheet.test.settingsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetWorkdayScreenDataUseCaseTest {

    @Test
    fun invoke_returnsAllDataWithCalculatedProjectTime() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projects = listOf(
                projectState(date = "2026-04-10", projectName = "Alpha", projectTime = "03:00"),
                projectState(date = "2026-04-10", projectName = "Beta", projectTime = "04:30"),
                projectState(date = "2026-04-11", projectName = "Gamma", projectTime = "07:00")
            )
            projectNames = listOf("Alpha", "Beta", "Gamma")
        }
        val settingsRepository = FakeSettingsRepository().apply {
            workTypes = listOf("Office", "Remote")
            globalSettings = settingsState(dailyWorkTimeEstimate = "07:30", initialFlexTimeTotal = "+04:15")
            effectiveSettings = globalSettings
            calculatedFlexTimeTotal = "-00:30"
        }
        val useCase = GetWorkdayScreenDataUseCase(
            projectRepository,
            settingsRepository
        )

        val result = useCase("2026-04-10")

        assertEquals("07:30", result.workTimeByDate)
        assertEquals("07:30", result.workTimeByDateEstimate)
        assertEquals("+04:15", result.initialFlexTimeTotal)
        assertEquals("03:45", result.flexTimeTotal)
        assertEquals(2, result.projects.size)
        assertEquals(3, result.projectNames.size)
    }

    @Test
    fun invoke_calculatesCombinedFlexFromProjectRows_evenWhenProjectDetailsFlexIsMissing() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projects = listOf(
                projectState(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00")
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            globalSettings = settingsState(dailyWorkTimeEstimate = "07:30", initialFlexTimeTotal = ZERO_TIME)
            effectiveSettings = globalSettings
            calculatedFlexTimeTotal = "00:30"
        }
        val useCase = GetWorkdayScreenDataUseCase(
            projectRepository = projectRepository,
            settingsRepository = settingsRepository
        )

        val result = useCase("2026-04-10")

        assertEquals("00:30", result.flexTimeTotal)
    }

    @Test
    fun invoke_calculatesCombinedFlexFromEditedProjectTime() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projects = listOf(
                projectState(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00")
            )
            projectNames = listOf("Alpha")
        }
        val settingsRepository = FakeSettingsRepository().apply {
            globalSettings = settingsState(dailyWorkTimeEstimate = "07:30", initialFlexTimeTotal = ZERO_TIME)
            effectiveSettings = globalSettings
            calculatedFlexTimeTotal = "00:30"
        }
        val useCase = GetWorkdayScreenDataUseCase(
            projectRepository = projectRepository,
            settingsRepository = settingsRepository
        )

        val result = useCase("2026-04-10")

        assertEquals("08:00", result.workTimeByDate)
        assertEquals("00:30", result.flexTimeTotal)
    }

    @Test
    fun invoke_usesZeroTimeWhenNoProjects() = runBlocking {
        val useCase = GetWorkdayScreenDataUseCase(
            projectRepository = FakeProjectRepository(),
            settingsRepository = FakeSettingsRepository()
        )

        val result = useCase("2026-04-10")

        assertEquals(ZERO_TIME, result.workTimeByDate)
        assertEquals("07:30", result.workTimeByDateEstimate)
        assertEquals(ZERO_TIME, result.initialFlexTimeTotal)
        assertEquals(ZERO_TIME, result.flexTimeTotal)
    }

    @Test
    fun invoke_whenEffectiveEstimateIsEmpty_fallsBackToGlobalEstimate() = runBlocking {
        val settingsRepository = FakeSettingsRepository().apply {
            globalSettings = settingsState(
                dailyWorkTimeEstimate = "08:00",
                initialFlexTimeTotal = ZERO_TIME
            )
            effectiveSettings = settingsState(
                dailyWorkTimeEstimate = "",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val useCase = GetWorkdayScreenDataUseCase(
            projectRepository = FakeProjectRepository(),
            settingsRepository = settingsRepository
        )

        val result = useCase("2026-04-10")

        assertEquals("08:00", result.workTimeByDateEstimate)
    }
}

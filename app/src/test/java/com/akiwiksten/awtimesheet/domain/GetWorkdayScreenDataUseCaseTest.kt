package com.akiwiksten.awtimesheet.domain

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.calculator.WorkTimeCalculator
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayStatsRow
import com.akiwiksten.awtimesheet.domain.usecase.GetWorkdayScreenDataUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class GetWorkdayScreenDataUseCaseTest {

    @Test
    fun invoke_returnsAllDataWithCalculatedProjectTime() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projects = listOf(
                SingleProjectState(date = "2026-04-10", projectName = "Alpha", projectTime = "03:00"),
                SingleProjectState(date = "2026-04-10", projectName = "Beta", projectTime = "04:30"),
                SingleProjectState(date = "2026-04-11", projectName = "Gamma", projectTime = "07:00")
            )
            projectNames = listOf("Alpha", "Beta", "Gamma")
        }
        val settingsRepository = FakeSettingsRepository().apply {
            workTypes = listOf("Office", "Remote")
            globalSettings = SettingsState(dailyWorkTimeEstimate = "07:30", initialFlexTimeTotal = "+04:15")
            effectiveSettings = globalSettings
            calculatedFlexTimeTotal = "-00:30"
        }
        val workdayRepository = FakeWorkdayRepository().apply {
            workdayStatsRows = listOf(
                WorkdayStatsRow(date = "2026-04-10", workTimeByDateEstimate = "07:30"),
                WorkdayStatsRow(date = "2026-04-11", workTimeByDateEstimate = "07:30")
            )
        }
        val useCase = GetWorkdayScreenDataUseCase(
            projectRepository,
            settingsRepository,
            workdayRepository
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
                SingleProjectState(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00")
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            globalSettings = SettingsState(dailyWorkTimeEstimate = "07:30", initialFlexTimeTotal = ZERO_TIME)
            effectiveSettings = globalSettings
            calculatedFlexTimeTotal = "00:30"
        }
        val workdayRepository = FakeWorkdayRepository().apply {
            workdayStatsRows = listOf(
                WorkdayStatsRow(date = "2026-04-10", workTimeByDateEstimate = "07:30")
            )
        }
        val useCase = GetWorkdayScreenDataUseCase(
            projectRepository = projectRepository,
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository
        )

        val result = useCase("2026-04-10")

        assertEquals("00:30", result.flexTimeTotal)
    }

    @Test
    fun invoke_calculatesCombinedFlexFromEditedProjectTime() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projects = listOf(
                SingleProjectState(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00")
            )
            projectNames = listOf("Alpha")
        }
        val settingsRepository = FakeSettingsRepository().apply {
            globalSettings = SettingsState(dailyWorkTimeEstimate = "07:30", initialFlexTimeTotal = ZERO_TIME)
            effectiveSettings = globalSettings
            calculatedFlexTimeTotal = "00:30"
        }
        val workdayRepository = FakeWorkdayRepository().apply {
            workdayStatsRows = listOf(
                WorkdayStatsRow(date = "2026-04-10", workTimeByDateEstimate = "07:30")
            )
        }
        val useCase = GetWorkdayScreenDataUseCase(
            projectRepository = projectRepository,
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository
        )

        val result = useCase("2026-04-10")

        assertEquals("08:00", result.workTimeByDate)
        assertEquals("00:30", result.flexTimeTotal)
    }

    @Test
    fun invoke_usesZeroTimeWhenNoProjects() = runBlocking {
        val useCase = GetWorkdayScreenDataUseCase(
            projectRepository = FakeProjectRepository(),
            settingsRepository = FakeSettingsRepository(),
            workdayRepository = FakeWorkdayRepository()
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
            globalSettings = SettingsState(
                dailyWorkTimeEstimate = "08:00",
                initialFlexTimeTotal = ZERO_TIME
            )
            effectiveSettings = SettingsState(
                dailyWorkTimeEstimate = "",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val useCase = GetWorkdayScreenDataUseCase(
            projectRepository = FakeProjectRepository(),
            settingsRepository = settingsRepository,
            workdayRepository = FakeWorkdayRepository()
        )

        val result = useCase("2026-04-10")

        assertEquals("08:00", result.workTimeByDateEstimate)
    }

    private class FakeProjectRepository : ProjectRepository {
        override suspend fun anyRecords(): Boolean = false

        var projects: List<SingleProjectState> = emptyList()
        var projectNames: List<String> = emptyList()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> {
            return projects.filter { it.date in start..end }
        }

        override suspend fun insertProject(project: SingleProjectState) = Unit

        override suspend fun deleteProject(project: SingleProjectState) = Unit

        override suspend fun getProjectNames(): List<String> = projectNames

        override suspend fun insertProjectName(projectName: String) = Unit

        override suspend fun deleteProjectName(projectName: String) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean = false

        override suspend fun getProject(date: String, projectName: String): SingleProjectState? = null

        override suspend fun getWorkTimeByDate(date: String): String =
            projects.filter { it.date == date }.fold(ZERO_TIME) { acc, p ->
                WorkTimeCalculator.calculateFlexTime(acc, p.projectTime)
            }
    }

    private class FakeSettingsRepository : SettingsRepository {
        var workTypes: List<String> = emptyList()
        var globalSettings: SettingsState? =
            SettingsState(
                dailyWorkTimeEstimate = "07:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        var effectiveSettings: SettingsState? = globalSettings
        var calculatedFlexTimeTotal: String = ZERO_TIME

        override suspend fun getSettings(): SettingsState? = globalSettings

        override suspend fun insertSettings(settings: SettingsState) = Unit

        override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? = effectiveSettings

        override suspend fun getWorkTypes(): List<String> = workTypes

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun deleteAllWorkTypes() = Unit

        override suspend fun getCalculatedFlextimeTotal(): String = calculatedFlexTimeTotal

        override suspend fun insertCalculatedFlextimeTotal(flexTime: String) {
            calculatedFlexTimeTotal = flexTime
        }
    }
    private class FakeWorkdayRepository : WorkdayRepository {
        var workdayStatsRows: List<WorkdayStatsRow> = emptyList()

        override suspend fun loadWorkday(date: String): String? = null

        override suspend fun upsertWorkdayStats(date: String, workTimeByDateEstimate: String) = Unit

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> =
            workdayStatsRows.filter { it.date in start..end }
    }
}

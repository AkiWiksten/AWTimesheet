package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.data.repository.WorkdayStatsRow
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import com.akiwiksten.worktime30.feature.settings.SettingsState
import com.akiwiksten.worktime30.feature.workday.SingleProjectState
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
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = WorkStatsState(dailyWorkTimeEstimate = "07:30", initialFlexTimeTotal = "+04:15")
            workdayStatsRows = listOf(
                WorkdayStatsRow(date = "2026-04-10", workTimeToday = "07:30", workTimeTodayEstimate = "07:30"),
                WorkdayStatsRow(date = "2026-04-11", workTimeToday = "07:00", workTimeTodayEstimate = "07:30")
            )
        }
        val useCase = GetWorkdayScreenDataUseCase(projectRepository, settingsRepository, projectDetailsRepository)

        val result = useCase("2026-04-10")

        assertEquals("07:30", result.projectTime)
        assertEquals("07:30", result.workTimeTodayEstimate)
        assertEquals("+04:15", result.initialFlexTimeTotal)
        assertEquals("03:45", result.calculatedFlexTimeTotal)
        assertEquals(2, result.projects.size)
        assertEquals(3, result.projectNames.size)
        assertEquals(listOf("Office", "Remote"), result.workTypes)
    }

    @Test
    fun invoke_calculatesCombinedFlexFromProjectRows_evenWhenProjectDetailsFlexIsMissing() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projects = listOf(
                SingleProjectState(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00")
            )
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = WorkStatsState(dailyWorkTimeEstimate = "07:30", initialFlexTimeTotal = ZERO_TIME)
            workdayStatsRows = listOf(
                WorkdayStatsRow(date = "2026-04-10", workTimeToday = "08:00", workTimeTodayEstimate = "07:30")
            )
        }
        val useCase = GetWorkdayScreenDataUseCase(
            projectRepository = projectRepository,
            settingsRepository = FakeSettingsRepository(),
            projectDetailsRepository = projectDetailsRepository
        )

        val result = useCase("2026-04-10")

        assertEquals("00:30", result.calculatedFlexTimeTotal)
    }

    @Test
    fun invoke_calculatesCombinedFlexFromEditedProjectTime() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            projects = listOf(
                SingleProjectState(date = "2026-04-10", projectName = "Alpha", projectTime = "08:00")
            )
            projectNames = listOf("Alpha")
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = WorkStatsState(dailyWorkTimeEstimate = "07:30", initialFlexTimeTotal = ZERO_TIME)
            workdayStatsRows = listOf(
                WorkdayStatsRow(date = "2026-04-10", workTimeToday = "08:00", workTimeTodayEstimate = "07:30")
            )
        }
        val useCase = GetWorkdayScreenDataUseCase(
            projectRepository = projectRepository,
            settingsRepository = FakeSettingsRepository(),
            projectDetailsRepository = projectDetailsRepository
        )

        val result = useCase("2026-04-10")

        assertEquals("08:00", result.projectTime)
        assertEquals("00:30", result.calculatedFlexTimeTotal)
    }

    @Test
    fun invoke_usesZeroTimeWhenNoProjects() = runBlocking {
        val useCase = GetWorkdayScreenDataUseCase(
            projectRepository = FakeProjectRepository(),
            settingsRepository = FakeSettingsRepository(),
            projectDetailsRepository = FakeProjectDetailsRepository()
        )

        val result = useCase("2026-04-10")

        assertEquals(ZERO_TIME, result.projectTime)
        assertEquals("07:30", result.workTimeTodayEstimate)
        assertEquals(ZERO_TIME, result.initialFlexTimeTotal)
        assertEquals(ZERO_TIME, result.calculatedFlexTimeTotal)
    }

    private class FakeProjectRepository : ProjectRepository {
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
    }

    private class FakeSettingsRepository : SettingsRepository {
        var workTypes: List<String> = emptyList()

        override suspend fun getSettings(): SettingsState? = null

        override suspend fun insertSettings(settings: SettingsState) = Unit

        override suspend fun getWorkTypes(): List<String> = workTypes

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun clearWorkTypes() = Unit
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        var workStats: WorkStatsState? = null
        var projectDetailsByDateRange: List<ProjectDetailsState> = emptyList()
        var workdayStatsRows: List<WorkdayStatsRow> = emptyList()

        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? = null

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun getWorkStats(): WorkStatsState? = workStats

        override suspend fun insertWorkStats(workStats: WorkStatsState) = Unit

        override suspend fun getWorkdayStatsByDateRange(start: String, end: String): List<WorkdayStatsRow> =
            workdayStatsRows.filter { it.date in start..end }

        override suspend fun getProjectDetailsByDateRange(start: String, end: String): List<ProjectDetailsState> =
            projectDetailsByDateRange
    }
}

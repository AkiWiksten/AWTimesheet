package com.akiwiksten.worktime30.feature.projects

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.DeleteWorkdayUseCase
import com.akiwiksten.worktime30.domain.GetWorkdayScreenDataUseCase
import com.akiwiksten.worktime30.domain.SaveWorkdayUseCase
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import com.akiwiksten.worktime30.feature.settings.SettingsState
import com.akiwiksten.worktime30.feature.workday.SingleProjectState
import com.akiwiksten.worktime30.feature.workday.WorkdayUiState
import com.akiwiksten.worktime30.feature.workday.WorkdayViewModel
import com.akiwiksten.worktime30.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkdayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun selectedDate_loadsProjectsAsSuccessState() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                SingleProjectState(
                    date = "2026-04-10",
                    projectName = "Beta",
                    projectTime = "02:30"
                )
            )
            projectNames = listOf("Beta", "Alpha")
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()
        projectDetailsRepository.workStats = WorkStatsState(dailyWorkTime = "07:30", initialFlexTimeTotal = "+01:45")
        projectDetailsRepository.projectDetailsByDateRange = listOf(
            ProjectDetailsState(date = "2026-04-10", projectName = "Beta", flexTimeToday = "-05:00")
        )
        val settingsRepository = FakeSettingsRepository().apply {
            workTypes = listOf("Office")
        }
        val dateRepository = DateRepository()
        dateRepository.updateDate("2026-04-10")

        val viewModel = createViewModel(projectRepository, projectDetailsRepository, settingsRepository, dateRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is WorkdayUiState.Success)
        state as WorkdayUiState.Success
        assertEquals("2026-04-10", state.date)
        assertEquals("02:30", state.workTimeToday)
        assertEquals("07:30", state.dailyWorkTime)
        assertEquals("-05:00", state.flexTimeToday)
        assertEquals("+01:45", state.initialFlexTimeTotal)
        assertEquals("-03:15", state.calculatedFlexTimeTotal)
        assertEquals(listOf("Alpha", "Beta"), state.projects.map { it.projectName })
    }

    @Test
    fun saveProject_persistsProjectAndReloadsData() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectNames = listOf("Alpha")
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository()
        dateRepository.updateDate("2026-04-10")

        val viewModel = createViewModel(projectRepository, projectDetailsRepository, settingsRepository, dateRepository)
        advanceUntilIdle()

        viewModel.saveProject(
            SingleProjectState(
                projectName = "Alpha",
                projectTime = "01:00",
            )
        )
        advanceUntilIdle()

        assertTrue(projectRepository.insertedProjects.any { it.projectName == "Alpha" })
        assertTrue(projectDetailsRepository.insertedProjectDetails.any { it.projectName == "Alpha" })
    }

    @Test
    fun saveProject_withNonZeroTime_updatesCalculatedFlexTimeTotal() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = WorkStatsState(dailyWorkTime = "07:30", lunchTime = "00:30", initialFlexTimeTotal = ZERO_TIME)
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository().apply {
            updateDate("2026-04-10")
        }

        val viewModel = createViewModel(projectRepository, projectDetailsRepository, settingsRepository, dateRepository)
        advanceUntilIdle()

        viewModel.saveProject(
            SingleProjectState(
                projectName = "Alpha",
                projectTime = "08:00"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as WorkdayUiState.Success
        assertEquals("00:30", state.calculatedFlexTimeTotal)
    }

    @Test
    fun saveProject_whenEditingExistingProjectTime_recalculatesCalculatedFlexTimeTotal() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                SingleProjectState(
                    date = "2026-04-10",
                    projectName = "Alpha",
                    projectTime = "02:00"
                )
            )
            projectNames = listOf("Alpha")
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = WorkStatsState(
                dailyWorkTime = "07:30",
                lunchTime = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository().apply {
            updateDate("2026-04-10")
        }

        val viewModel = createViewModel(projectRepository, projectDetailsRepository, settingsRepository, dateRepository)
        advanceUntilIdle()

        viewModel.saveProject(
            SingleProjectState(
                index = 0,
                projectName = "Alpha",
                projectTime = "08:00"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as WorkdayUiState.Success
        assertEquals("08:00", projectRepository.projectsByDateRange.single().projectTime)
        assertEquals("00:30", state.calculatedFlexTimeTotal)
    }

    @Test
    fun updateWorkStats_persistsDailyAndBalanceValues() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = WorkStatsState(dailyWorkTime = "07:30", lunchTime = "00:30", initialFlexTimeTotal = "+01:45")
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository()
        dateRepository.updateDate("2026-04-10")

        val viewModel = createViewModel(projectRepository, projectDetailsRepository, settingsRepository, dateRepository)
        advanceUntilIdle()

        viewModel.updateWorkStats(dailyWorkTime = "08:00", initialFlexTimeTotal = "-00:20")
        advanceUntilIdle()

        assertEquals("08:00", projectDetailsRepository.workStats?.dailyWorkTime)
        assertEquals("00:30", projectDetailsRepository.workStats?.lunchTime)
        assertEquals("-00:20", projectDetailsRepository.workStats?.initialFlexTimeTotal)
    }

    @Test
    fun updateWorkStats_invalidInput_doesNotPersist() = runTest {
        val projectRepository = FakeProjectRepository()
        val initialStats = WorkStatsState(dailyWorkTime = "07:30", lunchTime = "00:30", initialFlexTimeTotal = "+01:45")
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = initialStats
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository()
        dateRepository.updateDate("2026-04-10")

        val viewModel = createViewModel(projectRepository, projectDetailsRepository, settingsRepository, dateRepository)
        advanceUntilIdle()

        viewModel.updateWorkStats(dailyWorkTime = "8:00", initialFlexTimeTotal = "invalid")
        advanceUntilIdle()

        assertEquals(initialStats, projectDetailsRepository.workStats)
    }

    private fun createViewModel(
        projectRepository: FakeProjectRepository,
        projectDetailsRepository: FakeProjectDetailsRepository,
        settingsRepository: FakeSettingsRepository,
        dateRepository: DateRepository
    ): WorkdayViewModel {
        return WorkdayViewModel(
            getWorkdayScreenDataUseCase = GetWorkdayScreenDataUseCase(
                projectRepository = projectRepository,
                settingsRepository = settingsRepository,
                projectDetailsRepository = projectDetailsRepository
            ),
            saveWorkdayUseCase = SaveWorkdayUseCase(
                projectRepository = projectRepository,
                projectDetailsRepository = projectDetailsRepository
            ),
            deleteWorkdayUseCase = DeleteWorkdayUseCase(
                projectRepository = projectRepository,
                projectDetailsRepository = projectDetailsRepository
            ),
            projectDetailsRepository = projectDetailsRepository,
            dateRepository = dateRepository
        )
    }

    private class FakeProjectRepository : ProjectRepository {
        var projectsByDateRange: List<SingleProjectState> = emptyList()
        var projectNames: List<String> = emptyList()
        val insertedProjects = mutableListOf<SingleProjectState>()
        val deletedProjects = mutableListOf<SingleProjectState>()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> {
            return projectsByDateRange.filter { it.date in start..end }
        }

        override suspend fun insertProject(project: SingleProjectState) {
            insertedProjects += project
            projectsByDateRange = projectsByDateRange.filterNot {
                it.date == project.date && it.projectName == project.projectName
            } + project
        }

        override suspend fun deleteProject(project: SingleProjectState) {
            deletedProjects += project
            projectsByDateRange = projectsByDateRange.filterNot {
                it.date == project.date && it.projectName == project.projectName
            }
        }

        override suspend fun getProjectNames(): List<String> = projectNames

        override suspend fun insertProjectName(projectName: String) {
            if (this.projectNames.none { it == projectName }) {
                this.projectNames += projectName
            }
        }

        override suspend fun deleteProjectName(projectName: String) {
            this.projectNames = this.projectNames.filterNot { it == projectName }
        }

        override suspend fun isProjectNameUsed(projectName: String): Boolean {
            return projectsByDateRange.any { it.projectName == projectName }
        }
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        var workStats: WorkStatsState? = null
        var projectDetailsByDateRange: List<ProjectDetailsState> = emptyList()
        val insertedProjectDetails = mutableListOf<ProjectDetailsState>()

        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? {
            val state = insertedProjectDetails.firstOrNull { it.date == date && it.projectName == projectName }
            return state?.let {
                ProjectDetailsState(
                    date = it.date,
                    projectName = it.projectName,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    lunchStart = it.lunchStart,
                    lunchEnd = it.lunchEnd,
                    breakStart = it.breakStart,
                    breakEnd = it.breakEnd,
                    projectTime = it.projectTime,
                    flexTimeToday = it.flexTimeToday
                )
            }
        }

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) {
            insertedProjectDetails += projectDetails
        }

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) {
            insertedProjectDetails
                .removeIf {
                    it.date == projectDetails.date && it.projectName == projectDetails.projectName
                }
        }

        override suspend fun getWorkStats(): WorkStatsState? = workStats

        override suspend fun insertWorkStats(workStats: WorkStatsState) {
            this.workStats = workStats
        }

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsState> = projectDetailsByDateRange
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
}

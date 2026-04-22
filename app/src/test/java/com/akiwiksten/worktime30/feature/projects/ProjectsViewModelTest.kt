package com.akiwiksten.worktime30.feature.projects

import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.DeleteProjectsUseCase
import com.akiwiksten.worktime30.domain.GetProjectsScreenDataUseCase
import com.akiwiksten.worktime30.domain.SaveProjectsUseCase
import com.akiwiksten.worktime30.feature.projects.daily.ProjectsUiState
import com.akiwiksten.worktime30.feature.projects.daily.ProjectsViewModel
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import com.akiwiksten.worktime30.feature.settings.SettingsState
import com.akiwiksten.worktime30.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModelTest {

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
        projectDetailsRepository.workStats = WorkStatsState(dailyWorkTime = "07:30", balanceTotal = "+01:45")
        val settingsRepository = FakeSettingsRepository().apply {
            workTypes = listOf("Office")
        }
        val dateRepository = DateRepository()
        dateRepository.updateDate("2026-04-10")

        val viewModel = createViewModel(projectRepository, projectDetailsRepository, settingsRepository, dateRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is ProjectsUiState.Success)
        state as ProjectsUiState.Success
        assertEquals("2026-04-10", state.date)
        assertEquals("02:30", state.workTimeToday)
        assertEquals("07:30", state.dailyWorkTime)
        assertEquals("-05:00", state.balanceToday)
        assertEquals("+01:45", state.balanceTotal)
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
    fun updateWorkStats_persistsDailyAndBalanceValues() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = WorkStatsState(dailyWorkTime = "07:30", lunchTime = "00:30", balanceTotal = "+01:45")
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository()
        dateRepository.updateDate("2026-04-10")

        val viewModel = createViewModel(projectRepository, projectDetailsRepository, settingsRepository, dateRepository)
        advanceUntilIdle()

        viewModel.updateWorkStats(dailyWorkTime = "08:00", balanceTotal = "-00:20")
        advanceUntilIdle()

        assertEquals("08:00", projectDetailsRepository.workStats?.dailyWorkTime)
        assertEquals("00:30", projectDetailsRepository.workStats?.lunchTime)
        assertEquals("-00:20", projectDetailsRepository.workStats?.balanceTotal)
    }

    @Test
    fun updateWorkStats_invalidInput_doesNotPersist() = runTest {
        val projectRepository = FakeProjectRepository()
        val initialStats = WorkStatsState(dailyWorkTime = "07:30", lunchTime = "00:30", balanceTotal = "+01:45")
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = initialStats
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository()
        dateRepository.updateDate("2026-04-10")

        val viewModel = createViewModel(projectRepository, projectDetailsRepository, settingsRepository, dateRepository)
        advanceUntilIdle()

        viewModel.updateWorkStats(dailyWorkTime = "8:00", balanceTotal = "invalid")
        advanceUntilIdle()

        assertEquals(initialStats, projectDetailsRepository.workStats)
    }

    private fun createViewModel(
        projectRepository: FakeProjectRepository,
        projectDetailsRepository: FakeProjectDetailsRepository,
        settingsRepository: FakeSettingsRepository,
        dateRepository: DateRepository
    ): ProjectsViewModel {
        return ProjectsViewModel(
            getProjectsScreenDataUseCase = GetProjectsScreenDataUseCase(
                projectRepository = projectRepository,
                settingsRepository = settingsRepository,
                projectDetailsRepository = projectDetailsRepository
            ),
            saveProjectsUseCase = SaveProjectsUseCase(
                projectRepository = projectRepository,
                projectDetailsRepository = projectDetailsRepository
            ),
            deleteProjectsUseCase = DeleteProjectsUseCase(
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
                    balanceToday = it.balanceToday
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
        ): List<ProjectDetailsState> = emptyList()
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

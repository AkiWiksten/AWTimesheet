package com.akiwiksten.worktime30.feature.settings

import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.usecase.GetSettingsUseCase
import com.akiwiksten.worktime30.domain.usecase.GetWorkdayByMonthUseCase
import com.akiwiksten.worktime30.domain.usecase.SaveSettingsUseCase
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import com.akiwiksten.worktime30.feature.workday.SingleProjectState
import com.akiwiksten.worktime30.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadSettings_updatesSuccessState() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(name = "Aki", employer = "Company")
            workTypes = mutableListOf("Remote", "Office")
        }
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val viewModel = createViewModel(settingsRepository, projectRepository, projectDetailsRepository)

        viewModel.loadSettings()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.Success)
        state as SettingsUiState.Success
        assertEquals("Aki", state.data.name)
        assertEquals("Company", state.data.employer)
        assertEquals("00:00", state.data.lunchTimeEstimate)
        assertEquals(listOf("Office", "Remote"), state.data.workTypes)
    }

    @Test
    fun loadProjectsByMonth_setsEndOfMonthAndProjects() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(name = "Aki", employer = "Company")
        }
        val projectRepository = FakeProjectRepository().apply {
            projectsByRange["2026-04-01|2026-04-30"] = listOf(
                ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "03:00")
            )
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val viewModel = createViewModel(settingsRepository, projectRepository, projectDetailsRepository)

        viewModel.loadSettings()
        viewModel.loadProjectsByMonth("2026-04-10")
        advanceUntilIdle()

        val state = viewModel.uiState.value as SettingsUiState.Success
        assertEquals("2026-04-30", state.data.endMonthDate)
        assertEquals(1, state.data.projectsByMonth.size)
    }

    @Test
    fun saveSettings_persistsCurrentValues() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(name = "Aki", employer = "Company")
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val viewModel = createViewModel(settingsRepository, FakeProjectRepository(), projectDetailsRepository)

        viewModel.loadSettings()
        advanceUntilIdle()
        viewModel.setName("New Name")
        viewModel.addWorkType("Office")
        viewModel.saveSettings()
        advanceUntilIdle()

        assertEquals("New Name", settingsRepository.insertedSettings?.name)
        assertTrue(settingsRepository.insertedWorkTypes.any { it == "Office" })
    }

    private fun createViewModel(
        settingsRepository: FakeSettingsRepository,
        projectRepository: FakeProjectRepository,
        projectDetailsRepository: FakeProjectDetailsRepository
    ): SettingsViewModel {
        val dateRepository = DateRepository()
        return SettingsViewModel(
            getSettingsUseCase = GetSettingsUseCase(settingsRepository),
            saveSettingsUseCase = SaveSettingsUseCase(
                settingsRepository = settingsRepository,
                projectDetailsRepository = projectDetailsRepository,
                projectRepository = projectRepository,
                dateRepository = dateRepository
            ),
            getWorkdayByMonthUseCase = GetWorkdayByMonthUseCase(projectRepository),
            settingsRepository = settingsRepository,
            projectDetailsRepository = projectDetailsRepository,
            dateRepository = dateRepository
        )
    }

    private class FakeSettingsRepository : SettingsRepository {
        var settings: SettingsState? = null
        var workTypes: MutableList<String> = mutableListOf()
        val insertedWorkTypes = mutableListOf<String>()
        var insertedSettings: SettingsState? = null

        override suspend fun getSettings(): SettingsState? = settings

        override suspend fun insertSettings(settings: SettingsState) {
            insertedSettings = settings
        }

        override suspend fun getWorkTypes(): List<String> = workTypes

        override suspend fun insertWorkType(workType: String) {
            insertedWorkTypes += workType
            workTypes += workType
        }

        override suspend fun deleteWorkType(workType: String) {
            workTypes = workTypes.filterNot { it == workType }.toMutableList()
        }

        override suspend fun clearWorkTypes() {
            workTypes.clear()
        }
    }

    private class FakeProjectRepository : ProjectRepository {
        val projectsByRange = mutableMapOf<String, List<ProjectEntity>>()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> {
            return (projectsByRange["$start|$end"] ?: emptyList()).map { entity ->
                SingleProjectState(
                    projectName = entity.projectName,
                    projectTime = entity.projectTime,
                    kilometres = entity.kilometres.toString(),
                    allowance = entity.allowance,
                    workType = entity.workType
                )
            }
        }

        override suspend fun insertProject(project: SingleProjectState) = Unit

        override suspend fun deleteProject(project: SingleProjectState) = Unit

        override suspend fun getProjectNames(): List<String> = emptyList()

        override suspend fun insertProjectName(projectName: String) = Unit

        override suspend fun deleteProjectName(projectName: String) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean = false
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? = null

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun getWorkStats(): WorkStatsState? = null

        override suspend fun insertWorkStats(workStats: WorkStatsState) = Unit

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsState> = emptyList()
    }
}

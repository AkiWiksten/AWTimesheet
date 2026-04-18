package com.akiwiksten.worktime30.feature.settings

import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.GetProjectsByMonthUseCase
import com.akiwiksten.worktime30.domain.GetSettingsUseCase
import com.akiwiksten.worktime30.domain.SaveSettingsUseCase
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState
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
            settings = SettingsEntity(name = "Aki", employer = "Company")
            workTypes = mutableListOf(WorkTypeEntity("Remote"), WorkTypeEntity("Office"))
        }
        val projectRepository = FakeProjectRepository()
        val viewModel = createViewModel(settingsRepository, projectRepository)

        viewModel.loadSettings()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.Success)
        state as SettingsUiState.Success
        assertEquals("Aki", state.name)
        assertEquals("Company", state.employer)
        assertEquals(listOf("Office", "Remote"), state.workTypes)
    }

    @Test
    fun loadProjectsByMonth_setsEndOfMonthAndProjects() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsEntity(name = "Aki", employer = "Company")
        }
        val projectRepository = FakeProjectRepository().apply {
            projectsByRange["2026-04-01|2026-04-30"] = listOf(
                ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "03:00")
            )
        }
        val viewModel = createViewModel(settingsRepository, projectRepository)

        viewModel.loadSettings()
        viewModel.loadProjectsByMonth("2026-04-10")
        advanceUntilIdle()

        val state = viewModel.uiState.value as SettingsUiState.Success
        assertEquals("2026-04-30", state.endMonthDate)
        assertEquals(1, state.projectsByMonth.size)
    }

    @Test
    fun saveSettings_persistsCurrentValues() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsEntity(name = "Aki", employer = "Company")
        }
        val viewModel = createViewModel(settingsRepository, FakeProjectRepository())

        viewModel.loadSettings()
        advanceUntilIdle()
        viewModel.setName("New Name")
        viewModel.addWorkType("Office")
        viewModel.saveSettings()
        advanceUntilIdle()

        assertEquals("New Name", settingsRepository.insertedSettings?.name)
        assertTrue(settingsRepository.insertedWorkTypes.any { it.workType == "Office" })
    }

    private fun createViewModel(
        settingsRepository: FakeSettingsRepository,
        projectRepository: FakeProjectRepository
    ): SettingsViewModel {
        return SettingsViewModel(
            getSettingsUseCase = GetSettingsUseCase(settingsRepository),
            saveSettingsUseCase = SaveSettingsUseCase(settingsRepository),
            getProjectsByMonthUseCase = GetProjectsByMonthUseCase(projectRepository),
            settingsRepository = settingsRepository,
            dateRepository = DateRepository()
        )
    }

    private class FakeSettingsRepository : SettingsRepository {
        var settings: SettingsEntity? = null
        var workTypes: MutableList<WorkTypeEntity> = mutableListOf()
        val insertedWorkTypes = mutableListOf<WorkTypeEntity>()
        var insertedSettings: SettingsEntity? = null

        override suspend fun getSettings(): SettingsEntity? = settings

        override suspend fun insertSettings(settings: SettingsEntity) {
            insertedSettings = settings
        }

        override suspend fun getWorkTypes(): List<WorkTypeEntity> = workTypes

        override suspend fun insertWorkType(workType: WorkTypeEntity) {
            insertedWorkTypes += workType
            workTypes += workType
        }

        override suspend fun deleteWorkType(workType: WorkTypeEntity) {
            workTypes = workTypes.filterNot { it.workType == workType.workType }.toMutableList()
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

        override suspend fun insertProject(project: ProjectEntity) = Unit

        override suspend fun deleteProject(project: ProjectEntity) = Unit

        override suspend fun getProjectNames(): List<ProjectNameEntity> = emptyList()

        override suspend fun insertProjectName(projectName: ProjectNameEntity) = Unit

        override suspend fun deleteProjectName(projectName: ProjectNameEntity) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean = false
    }
}

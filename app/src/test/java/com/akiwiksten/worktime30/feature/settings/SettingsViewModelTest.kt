package com.akiwiksten.worktime30.feature.settings

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayStatsRow
import com.akiwiksten.worktime30.domain.usecase.GetProjectsByMonthUseCase
import com.akiwiksten.worktime30.domain.usecase.GetSettingsUseCase
import com.akiwiksten.worktime30.domain.usecase.SaveSettingsUseCase
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
        val viewModel = createViewModel(settingsRepository, projectRepository)

        viewModel.loadSettings()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.Success)
        state as SettingsUiState.Success
        assertEquals("Aki", state.data.name)
        assertEquals("Company", state.data.employer)
        assertEquals("00:00", state.data.dailyLunchTimeEstimate)
        assertEquals(listOf("Office", "Remote"), state.data.workTypes)
    }

    @Test
    fun refreshProjectsByMonth_setsEndOfMonthAndProjects() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(name = "Aki", employer = "Company")
        }
        val projectRepository = FakeProjectRepository().apply {
            projectsByRange["2026-04-01|2026-04-30"] = listOf(
                ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "03:00")
            )
        }
        val viewModel = createViewModel(settingsRepository, projectRepository)

        viewModel.loadSettings()
        viewModel.refreshProjectsByMonth("2026-04-10")
        advanceUntilIdle()

        val state = viewModel.uiState.value as SettingsUiState.Success
        assertEquals("2026-04-30", state.endMonthDate)
        assertEquals(1, state.projectsByMonth.size)
    }

    @Test
    fun selectedDateChange_refreshesMonthDataWithoutOverwritingUnsavedSettings() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(name = "Aki", employer = "Company")
        }
        val projectRepository = FakeProjectRepository().apply {
            projectsByRange["2026-04-01|2026-04-30"] = listOf(
                ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "03:00")
            )
            projectsByRange["2026-05-01|2026-05-31"] = listOf(
                ProjectEntity(date = "2026-05-10", projectName = "Beta", projectTime = "04:00")
            )
        }
        val dateRepository = DateRepository().apply { updateDate("2026-04-10") }
        val workdayRepository = FakeWorkdayRepository()
        val viewModel = SettingsViewModel(
            getSettingsUseCase = GetSettingsUseCase(settingsRepository),
            saveSettingsUseCase = SaveSettingsUseCase(
                settingsRepository = settingsRepository,
                workdayRepository = workdayRepository,
                projectRepository = projectRepository,
                dateRepository = dateRepository
            ),
            getProjectsByMonthUseCase = GetProjectsByMonthUseCase(projectRepository),
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )

        viewModel.loadSettings()
        advanceUntilIdle()
        viewModel.setName("Edited Name")

        dateRepository.updateDate("2026-05-10")
        advanceUntilIdle()

        val state = viewModel.uiState.value as SettingsUiState.Success
        assertEquals("Edited Name", state.data.name)
        assertEquals("2026-05-10", state.selectedDate)
        assertEquals("2026-05-31", state.endMonthDate)
        assertEquals(listOf("Beta"), state.projectsByMonth.map { it.projectName })
    }

    @Test
    fun saveSettings_persistsCurrentValues() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(name = "Aki", employer = "Company")
        }
        val viewModel = createViewModel(settingsRepository, FakeProjectRepository())

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
    ): SettingsViewModel {
        val dateRepository = DateRepository()
        val workdayRepository = FakeWorkdayRepository()
        return SettingsViewModel(
            getSettingsUseCase = GetSettingsUseCase(settingsRepository),
            saveSettingsUseCase = SaveSettingsUseCase(
                settingsRepository = settingsRepository,
                workdayRepository = workdayRepository,
                projectRepository = projectRepository,
                dateRepository = dateRepository
            ),
            getProjectsByMonthUseCase = GetProjectsByMonthUseCase(projectRepository),
            settingsRepository = settingsRepository,
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

        override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? = null

        override suspend fun getWorkTypes(): List<String> = workTypes

        override suspend fun insertWorkType(workType: String) {
            insertedWorkTypes += workType
            workTypes += workType
        }

        override suspend fun deleteWorkType(workType: String) {
            workTypes = workTypes.filterNot { it == workType }.toMutableList()
        }

        override suspend fun deleteAllWorkTypes() {
            workTypes.clear()
        }
    }

    private class FakeProjectRepository : ProjectRepository {
        val projectsByRange = mutableMapOf<String, List<ProjectEntity>>()

        override suspend fun anyRecords(): Boolean = false

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

        override suspend fun getWorkTimeByDate(date: String): String = ZERO_TIME
    }

    private class FakeWorkdayRepository : WorkdayRepository {
        override suspend fun loadWorkday(date: String): String? = null

        override suspend fun upsertWorkdayStats(date: String, workTimeByDateEstimate: String) = Unit

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> = emptyList()
    }
}

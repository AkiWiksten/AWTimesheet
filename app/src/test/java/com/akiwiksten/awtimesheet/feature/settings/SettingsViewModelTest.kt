package com.akiwiksten.awtimesheet.feature.settings

import com.akiwiksten.awtimesheet.data.database.entity.ProjectEntity
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.usecase.GetProjectsByMonthUseCase
import com.akiwiksten.awtimesheet.domain.usecase.GetSettingsUseCase
import com.akiwiksten.awtimesheet.domain.usecase.SaveSettingsUseCase
import com.akiwiksten.awtimesheet.test.FakeProjectRepository
import com.akiwiksten.awtimesheet.test.FakeSettingsRepository
import com.akiwiksten.awtimesheet.test.FakeWorkdayRepository
import com.akiwiksten.awtimesheet.test.MainDispatcherRule
import com.akiwiksten.awtimesheet.test.settingsState
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
            settings = settingsState(name = "Aki", employer = "Company")
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
    fun requestMonthlyReport_preservesLoadedsettingsState() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = settingsState(name = "Aki", employer = "Company")
        }
        val projectRepository = FakeProjectRepository().apply {
            dataByRange["2026-04-01|2026-04-30"] = listOf(
                ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "03:00")
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
            getProjectsByMonthUseCase = GetProjectsByMonthUseCase(
                projectRepository = projectRepository,
                settingsRepository = settingsRepository
            ),
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )

        viewModel.loadSettings()
        advanceUntilIdle()

        viewModel.requestMonthlyReport(name = "Aki", employer = "Company")
        advanceUntilIdle()

        val state = viewModel.uiState.value as SettingsUiState.Success
        assertEquals("Aki", state.data.name)
        assertEquals("Company", state.data.employer)
        assertEquals("2026-04-10", state.selectedDate)
    }

    @Test
    fun selectedDateChange_updatesSelectedDateWithoutOverwritingUnsavedSettings() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = settingsState(name = "Aki", employer = "Company")
        }
        val projectRepository = FakeProjectRepository().apply {
            dataByRange["2026-04-01|2026-04-30"] = listOf(
                ProjectEntity(date = "2026-04-10", projectName = "Alpha", projectTime = "03:00")
            )
            dataByRange["2026-05-01|2026-05-31"] = listOf(
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
            getProjectsByMonthUseCase = GetProjectsByMonthUseCase(
                projectRepository = projectRepository,
                settingsRepository = settingsRepository
            ),
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
    }

    @Test
    fun saveSettings_persistsCurrentValues() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = settingsState(name = "Aki", employer = "Company")
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
            getProjectsByMonthUseCase = GetProjectsByMonthUseCase(
                projectRepository = projectRepository,
                settingsRepository = settingsRepository
            ),
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
    }
}

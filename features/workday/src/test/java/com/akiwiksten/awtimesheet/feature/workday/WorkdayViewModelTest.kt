package com.akiwiksten.awtimesheet.feature.workday

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.test.InMemoryDateRepository
import com.akiwiksten.awtimesheet.domain.usecase.DeleteProjectUseCase
import com.akiwiksten.awtimesheet.domain.usecase.GetWorkdayScreenDataUseCase
import com.akiwiksten.awtimesheet.domain.usecase.UpdateSettingsUseCase
import com.akiwiksten.awtimesheet.test.FakeProjectDetailsRepository
import com.akiwiksten.awtimesheet.test.FakeProjectRepository
import com.akiwiksten.awtimesheet.test.FakeSettingsRepository
import com.akiwiksten.awtimesheet.test.FakeWorkdayRepository
import com.akiwiksten.awtimesheet.test.MainDispatcherRule
import com.akiwiksten.awtimesheet.test.projectDetailsState
import com.akiwiksten.awtimesheet.test.projectState
import com.akiwiksten.awtimesheet.test.settingsState
import com.akiwiksten.awtimesheet.test.workdayStatsRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class WorkdayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun selectedDate_loadsProjectsAsSuccessState() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                projectState(
                    date = "2026-04-10",
                    projectName = "Beta",
                    projectTime = "02:30"
                )
            )
            projectNames = listOf("Beta", "Alpha")
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()
        projectDetailsRepository.settings = settingsState(
            dailyWorkTimeEstimate = "07:30",
            initialFlexTimeTotal = "+01:45"
        )
        projectDetailsRepository.workdayStatsRows = listOf(
            workdayStatsRow(
                date = "2026-04-10",
                workTimeByDateEstimate = "07:30"
            )
        )
        projectDetailsRepository.projectDetailsByDateRange = listOf(
            projectDetailsState(date = "2026-04-10", projectName = "Beta")
        )
        val settingsRepository = FakeSettingsRepository().apply {
            workTypes = listOf("Office")
            calculatedFlexTimeTotal = "-05:00"
        }
        val dateRepository = InMemoryDateRepository()
        dateRepository.updateDate("2026-04-10")

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        Assert.assertTrue("Expected Success state but was $state", state is WorkdayUiState.Success)
        state as WorkdayUiState.Success
        Assert.assertEquals("2026-04-10", state.date)
        Assert.assertEquals("02:30", state.workTimeByDate)
        Assert.assertEquals("07:30", state.workTimeByDateEstimate)
        Assert.assertEquals("-05:00", state.flexTimeByDate)
        Assert.assertEquals("+01:45", state.initialFlexTimeTotal)
        Assert.assertEquals("-03:15", state.flexTimeTotal)
        Assert.assertEquals(listOf("Alpha", "Beta"), state.projects.map { it.projectName })
        Assert.assertEquals(ZERO_TIME, dateRepository.workTimeByDateChange.value)
    }

    @Test
    fun updateSettings_currentDayWithZeroWorkTime_updatesDailyAndBalanceValues() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:45"
            )
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = InMemoryDateRepository()
        dateRepository.updateDate(LocalDate.now().toString())

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        advanceUntilIdle()

        viewModel.updateSettings(workTimeByDateEstimate = "08:00")
        advanceUntilIdle()

        Assert.assertEquals("08:00", projectDetailsRepository.settings?.dailyWorkTimeEstimate)
        Assert.assertEquals("00:30", projectDetailsRepository.settings?.dailyLunchTimeEstimate)
        Assert.assertEquals("+01:45", projectDetailsRepository.settings?.initialFlexTimeTotal)
    }

    @Test
    fun updateSettings_currentDayWithNonZeroWorkTime_keepsExistingDailyWorkTime() = runTest {
        val today = LocalDate.now().toString()
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                projectState(
                    date = today,
                    projectName = "Alpha",
                    projectTime = "01:00"
                )
            )
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:45"
            )
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = InMemoryDateRepository().apply {
            updateDate(today)
        }

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        advanceUntilIdle()

        viewModel.updateSettings(workTimeByDateEstimate = "08:00")
        advanceUntilIdle()

        Assert.assertEquals("07:30", projectDetailsRepository.settings?.dailyWorkTimeEstimate)
        Assert.assertEquals("+01:45", projectDetailsRepository.settings?.initialFlexTimeTotal)
    }

    @Test
    fun updateSettings_nonCurrentDayWithZeroWorkTime_keepsExistingDailyWorkTime() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:45"
            )
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = InMemoryDateRepository().apply {
            updateDate("2000-01-01")
        }

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        advanceUntilIdle()

        viewModel.updateSettings(workTimeByDateEstimate = "08:00")
        advanceUntilIdle()

        Assert.assertEquals("07:30", projectDetailsRepository.settings?.dailyWorkTimeEstimate)
        Assert.assertEquals("+01:45", projectDetailsRepository.settings?.initialFlexTimeTotal)
    }

    @Test
    fun updateSettings_invalidInput_doesNotPersist() = runTest {
        val projectRepository = FakeProjectRepository()
        val initialStats = settingsState(
            dailyWorkTimeEstimate = "07:30",
            dailyLunchTimeEstimate = "00:30",
            initialFlexTimeTotal = "+01:45"
        )
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = initialStats
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = InMemoryDateRepository()
        dateRepository.updateDate("2026-04-10")

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        advanceUntilIdle()

        viewModel.updateSettings(workTimeByDateEstimate = "8:00")
        advanceUntilIdle()

        Assert.assertEquals(initialStats, projectDetailsRepository.settings)
    }

    @Test
    fun deleteProject_subtractsDeletedProjectTimeFromTrackedChange() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                projectState(
                    date = "2026-04-10",
                    projectName = "Alpha",
                    projectTime = "02:30"
                )
            )
            projectNames = listOf("Alpha")
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
            workdayStatsRows = listOf(
                workdayStatsRow(
                    date = "2026-04-10",
                    workTimeByDateEstimate = "07:30"
                )
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            calculatedFlexTimeTotal = "01:00"
        }
        val dateRepository = InMemoryDateRepository().apply {
            updateDate("2026-04-10")
            updateWorkTimeByDateChange("01:00")
        }

        val viewModel = createViewModel(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
        advanceUntilIdle()

        viewModel.deleteProject(
            projectState(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "02:30"
            )
        )
        advanceUntilIdle()

        Assert.assertEquals("-01:30", dateRepository.workTimeByDateChange.value)
        Assert.assertEquals("06:00", settingsRepository.calculatedFlexTimeTotal)
    }

    @Test
    fun reconcileFlexTimeTotalAfterProjectEditorReturn_addsFlexDeltaToPersistedCalculatedTotal() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                projectState(
                    date = "2026-04-10",
                    projectName = "Alpha",
                    projectTime = "02:30"
                )
            )
            projectNames = listOf("Alpha")
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            calculatedFlexTimeTotal = "01:00"
        }
        val dateRepository = InMemoryDateRepository().apply {
            updateDate("2026-04-10")
        }

        val viewModel = createViewModel(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
        advanceUntilIdle()

        projectRepository.projectsByDateRange = listOf(
            projectState(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "04:30"
            )
        )

        viewModel.reconcileFlexTimeTotalAfterProjectEditorReturn(
            oldFlexTimeByDate = "-05:00",
            oldWorkTimeByDate = "02:30"
        )
        advanceUntilIdle()

        Assert.assertEquals("03:00", settingsRepository.calculatedFlexTimeTotal)
        val state = viewModel.uiState.value as WorkdayUiState.Success
        Assert.assertEquals("03:00", state.flexTimeTotal)
    }

    @Test
    fun reconcileFlexTimeTotalAfterProjectEditorReturn_emptyDayToFirstProject_usesFlexByDateChange() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = emptyList()
            projectNames = listOf("Alpha")
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            calculatedFlexTimeTotal = ZERO_TIME
        }
        val dateRepository = InMemoryDateRepository().apply {
            updateDate("2026-04-10")
        }

        val viewModel = createViewModel(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
        advanceUntilIdle()

        projectRepository.projectsByDateRange = listOf(
            projectState(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "01:30"
            )
        )

        viewModel.reconcileFlexTimeTotalAfterProjectEditorReturn(
            oldFlexTimeByDate = "-07:30",
            oldWorkTimeByDate = ZERO_TIME
        )
        advanceUntilIdle()

        Assert.assertEquals("-06:00", settingsRepository.calculatedFlexTimeTotal)
        val state = viewModel.uiState.value as WorkdayUiState.Success
        Assert.assertEquals("-06:00", state.flexTimeTotal)
    }

    @Test
    fun reconcileFlexTimeTotalAfterProjectEditorReturn_usesTrackedWorkTimeDeltaWithoutExtraWorkdayRead() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                projectState(
                    date = "2026-04-10",
                    projectName = "Alpha",
                    projectTime = "02:30"
                )
            )
            projectNames = listOf("Alpha")
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            calculatedFlexTimeTotal = "01:00"
        }
        val dateRepository = InMemoryDateRepository().apply {
            updateDate("2026-04-10")
        }

        val viewModel = createViewModel(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
        advanceUntilIdle()

        val initialRangeReads = projectRepository.requestedRanges.size
        dateRepository.addWorkTimeByDateChange("02:00")

        viewModel.reconcileFlexTimeTotalAfterProjectEditorReturn(
            oldFlexTimeByDate = "-05:00",
            oldWorkTimeByDate = "02:30"
        )
        advanceUntilIdle()

        Assert.assertEquals(initialRangeReads + 1, projectRepository.requestedRanges.size)
        Assert.assertEquals("03:00", settingsRepository.calculatedFlexTimeTotal)
    }

    @Test
    fun newDateWithoutWorkdayRow_usesGlobalDailyEstimateForworkTimeByDateEstimate() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "08:00",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
            workdayStatsRows = emptyList()
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = InMemoryDateRepository().apply {
            updateDate("2026-04-12")
        }

        val viewModel = createViewModel(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as WorkdayUiState.Success
        Assert.assertEquals("2026-04-12", state.date)
        Assert.assertEquals(ZERO_TIME, state.workTimeByDate)
        Assert.assertEquals("08:00", state.workTimeByDateEstimate)
    }

    @Test
    fun dateWithEffectiveOverride_usesPerDayEstimateInsteadOfGlobal() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "08:00",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            effectiveSettings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val dateRepository = InMemoryDateRepository().apply {
            updateDate("2026-04-13")
        }

        val viewModel = createViewModel(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as WorkdayUiState.Success
        Assert.assertEquals("08:00", settingsRepository.settings?.dailyWorkTimeEstimate)
        Assert.assertEquals("07:30", state.workTimeByDateEstimate)
    }

    @Test
    fun updateSettings_saveGlobally_whenLocalUpdateBlocked_updatesGlobalEstimate() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                projectState(
                    date = "2000-01-01",
                    projectName = "Alpha",
                    projectTime = "01:00"
                )
            )
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:45"
            )
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = InMemoryDateRepository().apply {
            updateDate("2000-01-01")
        }

        val viewModel = createViewModel(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
        advanceUntilIdle()

        viewModel.updateSettings(workTimeByDateEstimate = "08:00", updateGlobalSettings = true)
        advanceUntilIdle()

        // Local/day value remains guarded due to non-current date and non-zero work time.
        Assert.assertEquals("07:30", projectDetailsRepository.settings?.dailyWorkTimeEstimate)
        // Global settings are still updated when user chooses Save globally.
        Assert.assertEquals("08:00", settingsRepository.settings?.dailyWorkTimeEstimate)
    }

    private fun createViewModel(
        projectRepository: FakeProjectRepository,
        projectDetailsRepository: FakeProjectDetailsRepository,
        settingsRepository: FakeSettingsRepository,
        dateRepository: DateRepository
    ): WorkdayViewModel {
        settingsRepository.settings = projectDetailsRepository.settings
        val workdayRepository = FakeWorkdayRepository(projectDetailsRepository).apply {
            workdayStatsRows = projectDetailsRepository.workdayStatsRows
        }
        return WorkdayViewModel(
            getWorkdayScreenDataUseCase = GetWorkdayScreenDataUseCase(
                projectRepository = projectRepository,
                settingsRepository = settingsRepository
            ),
            deleteProjectUseCase = DeleteProjectUseCase(
                projectRepository = projectRepository,
                projectDetailsRepository = projectDetailsRepository
            ),
            updateSettingsUseCase = UpdateSettingsUseCase(
                settingsRepository = settingsRepository,
                workdayRepository = workdayRepository
            ),
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
    }
}

package com.akiwiksten.awtimesheet.feature.workday

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.usecase.DeleteProjectUseCase
import com.akiwiksten.awtimesheet.domain.usecase.GetWorkdayScreenDataUseCase
import com.akiwiksten.awtimesheet.domain.usecase.SaveWorkdayUseCase
import com.akiwiksten.awtimesheet.domain.usecase.UpdateSettingsUseCase
import com.akiwiksten.awtimesheet.feature.workday.model.WorkdayUiState
import com.akiwiksten.awtimesheet.test.FakeProjectDetailsRepository
import com.akiwiksten.awtimesheet.test.FakeProjectRepository
import com.akiwiksten.awtimesheet.test.FakeSettingsRepository
import com.akiwiksten.awtimesheet.test.FakeWorkdayRepository
import com.akiwiksten.awtimesheet.test.InMemoryDateRepository
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
@Suppress("LargeClass", "LongMethod")
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
        viewModel.setLocalizedFlexDayWorkType("Absence-Flex day")
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
    fun selectedDate_withZeroWorkTime_showsZeroFlexTimeByDate() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:45"
            )
            workdayStatsRows = listOf(
                workdayStatsRow(
                    date = "2026-04-10",
                    workTimeByDateEstimate = "07:30"
                )
            )
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = InMemoryDateRepository().apply {
            updateDate("2026-04-10")
        }

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        viewModel.setLocalizedFlexDayWorkType("Absence-Flex day")
        advanceUntilIdle()

        val state = viewModel.uiState.value as WorkdayUiState.Success
        Assert.assertEquals(ZERO_TIME, state.workTimeByDate)
        Assert.assertEquals(ZERO_TIME, state.flexTimeByDate)
    }

    @Test
    fun selectedDate_withAbsenceFlexDay_setsFlexTimeByDateToNegativeProjectTime() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                projectState(
                    date = "2026-04-10",
                    projectName = "Absence-Flex day",
                    projectTime = "07:30",
                    workType = "Absence-Flex day"
                )
            )
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            workTypes = listOf("Absence-Flex day")
        }
        val dateRepository = InMemoryDateRepository().apply {
            updateDate("2026-04-10")
        }

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        viewModel.setLocalizedFlexDayWorkType("Absence-Flex day")
        advanceUntilIdle()

        val state = viewModel.uiState.value as WorkdayUiState.Success
        Assert.assertEquals("07:30", state.workTimeByDate)
        Assert.assertEquals("-07:30", state.flexTimeByDate)
    }

    @Test
    fun selectedDate_withAbsenceFlexDayProjectNameAndEmptyWorkType_setsFlexTimeByDateToNegativeProjectTime() =
        runTest {
            val projectRepository = FakeProjectRepository().apply {
                projectsByDateRange = listOf(
                    projectState(
                        date = "2026-04-10",
                        projectName = "Absence-Flex day",
                        projectTime = "07:30",
                        workType = ""
                    )
                )
            }
            val projectDetailsRepository = FakeProjectDetailsRepository().apply {
                settings = settingsState(
                    dailyWorkTimeEstimate = "07:30",
                    dailyLunchTimeEstimate = "00:30",
                    initialFlexTimeTotal = ZERO_TIME
                )
            }
            val settingsRepository = FakeSettingsRepository().apply {
                workTypes = listOf("Absence-Flex day")
            }
            val dateRepository = InMemoryDateRepository().apply {
                updateDate("2026-04-10")
            }

            val viewModel = createViewModel(
                projectRepository,
                projectDetailsRepository,
                settingsRepository,
                dateRepository
            )
            viewModel.setLocalizedFlexDayWorkType("Absence-Flex day")
            advanceUntilIdle()

            val state = viewModel.uiState.value as WorkdayUiState.Success
            Assert.assertEquals("07:30", state.workTimeByDate)
            Assert.assertEquals("-07:30", state.flexTimeByDate)
        }

    @Test
    fun selectedDate_withSwedishAbsenceFlexdagLabel_setsFlexTimeByDateToNegativeProjectTime() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                projectState(
                    date = "2026-04-10",
                    projectName = "Frånvaro-Flexdag",
                    projectTime = "07:30",
                    workType = "Frånvaro-Flexdag"
                )
            )
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            workTypes = listOf("Frånvaro-Flexdag")
        }
        val dateRepository = InMemoryDateRepository().apply {
            updateDate("2026-04-10")
        }

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        viewModel.setLocalizedFlexDayWorkType("Frånvaro-Flexdag")
        advanceUntilIdle()

        val state = viewModel.uiState.value as WorkdayUiState.Success
        Assert.assertEquals("07:30", state.workTimeByDate)
        Assert.assertEquals("-07:30", state.flexTimeByDate)
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
    fun updateSettings_currentDayWithNonZeroWorkTime_updatesDailyWorkTime() = runTest {
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

        Assert.assertEquals("08:00", projectDetailsRepository.settings?.dailyWorkTimeEstimate)
        Assert.assertEquals("+01:45", projectDetailsRepository.settings?.initialFlexTimeTotal)
    }

    @Test
    fun updateSettings_nonCurrentDayWithZeroWorkTime_updatesDailyWorkTime() = runTest {
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

        Assert.assertEquals("08:00", projectDetailsRepository.settings?.dailyWorkTimeEstimate)
        Assert.assertEquals("+01:45", projectDetailsRepository.settings?.initialFlexTimeTotal)
    }

    @Test
    fun updateSettings_whenEstimateChanges_updatesFlexTimeTotal() = runTest {
        val today = LocalDate.now().toString()
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                projectState(
                    date = today,
                    projectName = "Alpha",
                    projectTime = "02:00"
                )
            )
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            calculatedFlexTimeTotal = "-05:30"
        }
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

        Assert.assertEquals("-06:00", settingsRepository.calculatedFlexTimeTotal)
        Assert.assertEquals("08:00", projectDetailsRepository.settings?.dailyWorkTimeEstimate)
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
    fun deleteProject_withRecordedZeroTimeItem_nullifiesProjectWithoutDeletingProjectName() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                projectState(
                    date = "2026-04-10",
                    projectName = "Alpha",
                    projectTime = ZERO_TIME
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
                projectTime = ZERO_TIME
            )
        )
        advanceUntilIdle()

        Assert.assertEquals("01:00", dateRepository.workTimeByDateChange.value)
        Assert.assertEquals(emptyList<String>(), projectRepository.deletedProjectNames)
        Assert.assertEquals(
            listOf(projectState(date = "2026-04-10", projectName = "Alpha", projectTime = ZERO_TIME)),
            projectRepository.deletedProjects
        )
        Assert.assertEquals(
            listOf(projectDetailsState(date = "2026-04-10", projectName = "Alpha")),
            projectDetailsRepository.deletedProjectDetails
        )
        Assert.assertEquals("01:00", settingsRepository.calculatedFlexTimeTotal)
    }

    @Test
    fun deleteProject_withPlaceholderItem_deletesOnlyProjectName() = runTest {
        val projectRepository = FakeProjectRepository().apply {
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

        viewModel.deleteProject(projectState(projectName = "Alpha", projectTime = ZERO_TIME))
        advanceUntilIdle()

        Assert.assertEquals(listOf("Alpha"), projectRepository.deletedProjectNames)
        Assert.assertEquals(emptyList<com.akiwiksten.awtimesheet.domain.model.SingleProjectState>(), projectRepository.deletedProjects)
        Assert.assertEquals(emptyList<com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState>(), projectDetailsRepository.deletedProjectDetails)
        Assert.assertEquals("01:00", dateRepository.workTimeByDateChange.value)
        Assert.assertEquals("01:00", settingsRepository.calculatedFlexTimeTotal)
    }

    @Test
    fun reconcileAfterProjectEditorReturn_addsFlexDeltaToPersistedCalculatedTotal() = runTest {
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
    fun reconcileAfterProjectEditorReturn_emptyDayToFirstProject_usesFlexByDateChange() = runTest {
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
    fun reconcileAfterProjectEditorReturn_readsLatestWorkdayDataBeforeReload() = runTest {
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

        Assert.assertEquals(initialRangeReads + 2, projectRepository.requestedRanges.size)
        Assert.assertEquals("01:00", settingsRepository.calculatedFlexTimeTotal)
    }

    @Test
    fun reconcileAfterReturn_cumulativeTrackedChange_keepsTotalFromLatestPersistedWorkTime() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = emptyList()
            projectNames = listOf("Alpha", "Beta")
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

        // First added project: day total work time becomes 01:00.
        projectRepository.projectsByDateRange = listOf(
            projectState(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "01:00"
            )
        )
        dateRepository.addWorkTimeByDateChange("01:00")

        viewModel.reconcileFlexTimeTotalAfterProjectEditorReturn(
            oldFlexTimeByDate = "-07:30",
            oldWorkTimeByDate = ZERO_TIME
        )
        advanceUntilIdle()

        // Second added project: cumulative tracked change becomes 03:00, real day total is 03:00.
        projectRepository.projectsByDateRange = listOf(
            projectState(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "01:00"
            ),
            projectState(
                date = "2026-04-10",
                projectName = "Beta",
                projectTime = "02:00"
            )
        )
        dateRepository.addWorkTimeByDateChange("02:00")

        viewModel.reconcileFlexTimeTotalAfterProjectEditorReturn(
            oldFlexTimeByDate = "-06:30",
            oldWorkTimeByDate = "01:00"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as WorkdayUiState.Success
        Assert.assertEquals("03:00", state.workTimeByDate)
        Assert.assertEquals("-04:30", state.flexTimeByDate)
        Assert.assertEquals("-04:30", state.flexTimeTotal)
        Assert.assertEquals("03:00", dateRepository.workTimeByDateChange.value)
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
    fun updateSettings_saveGlobally_withLoggedWork_updatesLocalAndGlobalEstimate() = runTest {
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

        // Local/day value follows user input.
        Assert.assertEquals("08:00", projectDetailsRepository.settings?.dailyWorkTimeEstimate)
        // Global settings are also updated when user chooses Save globally.
        Assert.assertEquals("08:00", settingsRepository.settings?.dailyWorkTimeEstimate)
    }

    @Test
    fun addTwoProjectsOnEmptyDay_reconcileAfterEachSave_keepsFlexTotalConsistent() = runTest {
        val date = "2026-04-10"
        val projectRepository = FakeProjectRepository().apply {
            projectNames = listOf("Alpha", "Beta")
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
            updateDate(date)
        }

        val workdayRepository = FakeWorkdayRepository(projectDetailsRepository).apply {
            workdayStatsRows = projectDetailsRepository.workdayStatsRows
        }
        val saveWorkdayUseCase = SaveWorkdayUseCase(
            projectRepository = projectRepository,
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository,
            projectDetailsRepository = projectDetailsRepository
        )

        val viewModel = createViewModel(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
        advanceUntilIdle()

        val beforeFirstAdd = viewModel.uiState.value as WorkdayUiState.Success
        saveWorkdayUseCase(
            projectToSave = projectState(date = date, projectName = "Alpha", projectTime = "01:00"),
            projectDetailsToSave = projectDetailsState(date = date, projectName = "Alpha", projectTime = "01:00")
        )
        dateRepository.addWorkTimeByDateChange("01:00")
        viewModel.reconcileFlexTimeTotalAfterProjectEditorReturn(
            oldFlexTimeByDate = beforeFirstAdd.flexTimeByDate,
            oldWorkTimeByDate = beforeFirstAdd.workTimeByDate
        )
        advanceUntilIdle()

        val beforeSecondAdd = viewModel.uiState.value as WorkdayUiState.Success
        saveWorkdayUseCase(
            projectToSave = projectState(date = date, projectName = "Beta", projectTime = "02:00"),
            projectDetailsToSave = projectDetailsState(date = date, projectName = "Beta", projectTime = "02:00")
        )
        dateRepository.addWorkTimeByDateChange("02:00")
        viewModel.reconcileFlexTimeTotalAfterProjectEditorReturn(
            oldFlexTimeByDate = beforeSecondAdd.flexTimeByDate,
            oldWorkTimeByDate = beforeSecondAdd.workTimeByDate
        )
        advanceUntilIdle()

        val finalState = viewModel.uiState.value as WorkdayUiState.Success
        Assert.assertEquals("03:00", finalState.workTimeByDate)
        Assert.assertEquals("-04:30", finalState.flexTimeByDate)
        Assert.assertEquals("-04:30", finalState.flexTimeTotal)
    }

    @Test
    fun calendarRefresh_afterSettingsChange_updatesInitialAndTotalFlexTime() = runTest {
        val date = "2026-04-10"
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:00"
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:00"
            )
            calculatedFlexTimeTotal = "-02:00"
        }
        val dateRepository = InMemoryDateRepository().apply { updateDate(date) }

        val viewModel = createViewModel(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
        advanceUntilIdle()

        var state = viewModel.uiState.value as WorkdayUiState.Success
        Assert.assertEquals("+01:00", state.initialFlexTimeTotal)
        Assert.assertEquals("-01:00", state.flexTimeTotal)

        settingsRepository.settings = settingsRepository.settings?.copy(initialFlexTimeTotal = "00:00")
        dateRepository.notifyCalendarDataChanged()
        advanceUntilIdle()

        state = viewModel.uiState.value as WorkdayUiState.Success
        Assert.assertEquals("00:00", state.initialFlexTimeTotal)
        Assert.assertEquals("-02:00", state.flexTimeTotal)
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

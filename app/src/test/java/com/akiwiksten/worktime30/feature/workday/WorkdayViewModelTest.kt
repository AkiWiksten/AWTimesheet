package com.akiwiksten.worktime30.feature.workday

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayRepository
import com.akiwiksten.worktime30.domain.repository.WorkdayStatsRow
import com.akiwiksten.worktime30.domain.usecase.DeleteWorkdayUseCase
import com.akiwiksten.worktime30.domain.usecase.GetWorkdayScreenDataUseCase
import com.akiwiksten.worktime30.domain.usecase.SaveWorkdayUseCase
import com.akiwiksten.worktime30.test.MainDispatcherRule
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
                SingleProjectState(
                    date = "2026-04-10",
                    projectName = "Beta",
                    projectTime = "02:30"
                )
            )
            projectNames = listOf("Beta", "Alpha")
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()
        projectDetailsRepository.workStats = SettingsState(
            dailyWorkTimeEstimate = "07:30",
            initialFlexTimeTotal = "+01:45"
        )
        projectDetailsRepository.workdayStatsRows = listOf(
            WorkdayStatsRow(
                date = "2026-04-10",
                workTimeTodayEstimate = "07:30"
            )
        )
        projectDetailsRepository.projectDetailsByDateRange = listOf(
            ProjectDetailsState(date = "2026-04-10", projectName = "Beta")
        )
        val settingsRepository = FakeSettingsRepository().apply {
            workTypes = listOf("Office")
        }
        val dateRepository = DateRepository()
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
        Assert.assertEquals("02:30", state.workTimeToday)
        Assert.assertEquals("07:30", state.workTimeTodayEstimate)
        Assert.assertEquals("-05:00", state.flexTimeToday)
        Assert.assertEquals("+01:45", state.initialFlexTimeTotal)
        Assert.assertEquals("-03:15", state.calculatedFlexTimeTotal)
        Assert.assertEquals(listOf("Alpha", "Beta"), state.projects.map { it.projectName })
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

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        advanceUntilIdle()

        viewModel.saveProject(
            SingleProjectState(
                projectName = "Alpha",
                projectTime = "01:00",
            )
        )
        advanceUntilIdle()

        Assert.assertTrue(projectRepository.insertedProjects.any { it.projectName == "Alpha" })
        Assert.assertTrue(projectDetailsRepository.insertedProjectDetails.any { it.projectName == "Alpha" })
    }

    @Test
    fun saveProject_withNonZeroTime_updatesCalculatedFlexTimeTotal() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository().apply {
            updateDate("2026-04-10")
        }

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        advanceUntilIdle()

        viewModel.saveProject(
            SingleProjectState(
                projectName = "Alpha",
                projectTime = "08:00"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as WorkdayUiState.Success
        Assert.assertEquals("00:30", state.calculatedFlexTimeTotal)
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
            workStats = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository().apply {
            updateDate("2026-04-10")
        }

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
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
        Assert.assertEquals("08:00", projectRepository.projectsByDateRange.single().projectTime)
        Assert.assertEquals("00:30", state.calculatedFlexTimeTotal)
    }

    @Test
    fun updateWorkStats_currentDayWithZeroWorkTime_updatesDailyAndBalanceValues() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:45"
            )
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository()
        dateRepository.updateDate(LocalDate.now().toString())

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        advanceUntilIdle()

        viewModel.updateWorkStats(workTimeTodayEstimate = "08:00", initialFlexTimeTotal = "-00:20")
        advanceUntilIdle()

        Assert.assertEquals("08:00", projectDetailsRepository.workStats?.dailyWorkTimeEstimate)
        Assert.assertEquals("00:30", projectDetailsRepository.workStats?.dailyLunchTimeEstimate)
        Assert.assertEquals("-00:20", projectDetailsRepository.workStats?.initialFlexTimeTotal)
    }

    @Test
    fun updateWorkStats_currentDayWithNonZeroWorkTime_keepsExistingDailyWorkTime() = runTest {
        val today = LocalDate.now().toString()
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                SingleProjectState(
                    date = today,
                    projectName = "Alpha",
                    projectTime = "01:00"
                )
            )
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:45"
            )
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository().apply {
            updateDate(today)
        }

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        advanceUntilIdle()

        viewModel.updateWorkStats(workTimeTodayEstimate = "08:00", initialFlexTimeTotal = "-00:20")
        advanceUntilIdle()

        Assert.assertEquals("07:30", projectDetailsRepository.workStats?.dailyWorkTimeEstimate)
        Assert.assertEquals("-00:20", projectDetailsRepository.workStats?.initialFlexTimeTotal)
    }

    @Test
    fun updateWorkStats_nonCurrentDayWithZeroWorkTime_keepsExistingDailyWorkTime() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "+01:45"
            )
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository().apply {
            updateDate("2000-01-01")
        }

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        advanceUntilIdle()

        viewModel.updateWorkStats(workTimeTodayEstimate = "08:00", initialFlexTimeTotal = "-00:20")
        advanceUntilIdle()

        Assert.assertEquals("07:30", projectDetailsRepository.workStats?.dailyWorkTimeEstimate)
        Assert.assertEquals("-00:20", projectDetailsRepository.workStats?.initialFlexTimeTotal)
    }

    @Test
    fun updateWorkStats_invalidInput_doesNotPersist() = runTest {
        val projectRepository = FakeProjectRepository()
        val initialStats = SettingsState(
            dailyWorkTimeEstimate = "07:30",
            dailyLunchTimeEstimate = "00:30",
            initialFlexTimeTotal = "+01:45"
        )
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            workStats = initialStats
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository()
        dateRepository.updateDate("2026-04-10")

        val viewModel = createViewModel(
            projectRepository,
            projectDetailsRepository,
            settingsRepository,
            dateRepository
        )
        advanceUntilIdle()

        viewModel.updateWorkStats(workTimeTodayEstimate = "8:00", initialFlexTimeTotal = "invalid")
        advanceUntilIdle()

        Assert.assertEquals(initialStats, projectDetailsRepository.workStats)
    }

    private fun createViewModel(
        projectRepository: FakeProjectRepository,
        projectDetailsRepository: FakeProjectDetailsRepository,
        settingsRepository: FakeSettingsRepository,
        dateRepository: DateRepository
    ): WorkdayViewModel {
        settingsRepository.workStats = projectDetailsRepository.workStats
        val workdayRepository = FakeWorkdayRepository(projectDetailsRepository).apply {
            workdayStatsRows = projectDetailsRepository.workdayStatsRows
        }
        return WorkdayViewModel(
            getWorkdayScreenDataUseCase = GetWorkdayScreenDataUseCase(
                projectRepository = projectRepository,
                settingsRepository = settingsRepository,
                workdayRepository = workdayRepository
            ),
            saveWorkdayUseCase = SaveWorkdayUseCase(
                projectRepository = projectRepository,
                projectDetailsRepository = projectDetailsRepository,
                settingsRepository = settingsRepository,
                workdayRepository = workdayRepository
            ),
            deleteWorkdayUseCase = DeleteWorkdayUseCase(
                projectRepository = projectRepository,
                projectDetailsRepository = projectDetailsRepository
            ),
            settingsRepository = settingsRepository,
            workdayRepository = workdayRepository,
            dateRepository = dateRepository
        )
    }

    private class FakeProjectRepository : ProjectRepository {
        var projectsByDateRange: List<SingleProjectState> = emptyList()
        var projectNames: List<String> = emptyList()
        val insertedProjects = mutableListOf<SingleProjectState>()
        val deletedProjects = mutableListOf<SingleProjectState>()

        override suspend fun anyRecords(): Boolean = false

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

        override suspend fun getProjectTimeSumByDate(date: String): String =
            projectsByDateRange.filter { it.date == date }.fold(ZERO_TIME) { acc, p ->
                WorkTimeCalculator.calculateFlexTime(acc, p.projectTime)
            }
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        var workStats: SettingsState? = null
        var projectDetailsByDateRange: List<ProjectDetailsState> = emptyList()
        var workdayStatsRows: List<WorkdayStatsRow> = emptyList()
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
                    projectTime = it.projectTime
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

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsState> = projectDetailsByDateRange
    }

    private class FakeWorkdayRepository(
        private val projectDetailsRepository: FakeProjectDetailsRepository? = null
    ) : WorkdayRepository {
        var workdayStatsRows: List<WorkdayStatsRow> = emptyList()

        override suspend fun loadWorkday(date: String): SettingsState? = null

        override suspend fun upsertWorkdayStats(date: String, settingsEstimates: SettingsState) {
            val updatedRow = WorkdayStatsRow(
                date = date,
                workTimeTodayEstimate = settingsEstimates.dailyWorkTimeEstimate
            )
            workdayStatsRows = workdayStatsRows.filterNot { it.date == date } + updatedRow
            projectDetailsRepository?.workdayStatsRows = workdayStatsRows
            projectDetailsRepository?.workStats = settingsEstimates
        }

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> {
            return workdayStatsRows.filter { it.date in start..end }
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        var workTypes: List<String> = emptyList()
        var workStats: SettingsState? = null

        override suspend fun getSettings(): SettingsState? = null

        override suspend fun insertSettings(settings: SettingsState) = Unit

        override suspend fun getGlobalSettingsEstimates(): SettingsState? = workStats

        override suspend fun saveGlobalSettingsEstimates(estimates: SettingsState) {
            this.workStats = estimates
        }

        override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? = getGlobalSettingsEstimates()

        override suspend fun getWorkTypes(): List<String> = workTypes

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun clearWorkTypes() = Unit
    }
}

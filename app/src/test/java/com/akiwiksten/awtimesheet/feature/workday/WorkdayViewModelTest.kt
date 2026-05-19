package com.akiwiksten.awtimesheet.feature.workday

import com.akiwiksten.awtimesheet.core.ZERO_TIME
import com.akiwiksten.awtimesheet.core.calculator.WorkTimeCalculator
import com.akiwiksten.awtimesheet.domain.model.ProjectDetailsState
import com.akiwiksten.awtimesheet.domain.model.SettingsState
import com.akiwiksten.awtimesheet.domain.model.SingleProjectState
import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectDetailsRepository
import com.akiwiksten.awtimesheet.domain.repository.ProjectRepository
import com.akiwiksten.awtimesheet.domain.repository.SettingsRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayRepository
import com.akiwiksten.awtimesheet.domain.repository.WorkdayStatsRow
import com.akiwiksten.awtimesheet.domain.usecase.DeleteProjectUseCase
import com.akiwiksten.awtimesheet.domain.usecase.GetWorkdayScreenDataUseCase
import com.akiwiksten.awtimesheet.domain.usecase.UpdateSettingsUseCase
import com.akiwiksten.awtimesheet.test.MainDispatcherRule
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
        projectDetailsRepository.settings = SettingsState(
            dailyWorkTimeEstimate = "07:30",
            initialFlexTimeTotal = "+01:45"
        )
        projectDetailsRepository.workdayStatsRows = listOf(
            WorkdayStatsRow(
                date = "2026-04-10",
                workTimeByDateEstimate = "07:30"
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
        Assert.assertEquals("02:30", state.workTimeByDate)
        Assert.assertEquals("07:30", state.workTimeByDateEstimate)
        Assert.assertEquals("-05:00", state.flexTimeByDate)
        Assert.assertEquals("+01:45", state.initialFlexTimeTotal)
        Assert.assertEquals("-03:15", state.calculatedFlexTimeTotal)
        Assert.assertEquals(listOf("Alpha", "Beta"), state.projects.map { it.projectName })
        Assert.assertEquals(ZERO_TIME, dateRepository.workTimeByDateChange.value)
    }

    @Test
    fun updateSettings_currentDayWithZeroWorkTime_updatesDailyAndBalanceValues() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = SettingsState(
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
                SingleProjectState(
                    date = today,
                    projectName = "Alpha",
                    projectTime = "01:00"
                )
            )
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = SettingsState(
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

        viewModel.updateSettings(workTimeByDateEstimate = "08:00")
        advanceUntilIdle()

        Assert.assertEquals("07:30", projectDetailsRepository.settings?.dailyWorkTimeEstimate)
        Assert.assertEquals("+01:45", projectDetailsRepository.settings?.initialFlexTimeTotal)
    }

    @Test
    fun updateSettings_nonCurrentDayWithZeroWorkTime_keepsExistingDailyWorkTime() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = SettingsState(
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

        viewModel.updateSettings(workTimeByDateEstimate = "08:00")
        advanceUntilIdle()

        Assert.assertEquals("07:30", projectDetailsRepository.settings?.dailyWorkTimeEstimate)
        Assert.assertEquals("+01:45", projectDetailsRepository.settings?.initialFlexTimeTotal)
    }

    @Test
    fun updateSettings_invalidInput_doesNotPersist() = runTest {
        val projectRepository = FakeProjectRepository()
        val initialStats = SettingsState(
            dailyWorkTimeEstimate = "07:30",
            dailyLunchTimeEstimate = "00:30",
            initialFlexTimeTotal = "+01:45"
        )
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = initialStats
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

        viewModel.updateSettings(workTimeByDateEstimate = "8:00")
        advanceUntilIdle()

        Assert.assertEquals(initialStats, projectDetailsRepository.settings)
    }

    @Test
    fun deleteProject_subtractsDeletedProjectTimeFromTrackedChange() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(
                SingleProjectState(
                    date = "2026-04-10",
                    projectName = "Alpha",
                    projectTime = "02:30"
                )
            )
            projectNames = listOf("Alpha")
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
            workdayStatsRows = listOf(
                WorkdayStatsRow(
                    date = "2026-04-10",
                    workTimeByDateEstimate = "07:30"
                )
            )
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository().apply {
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
            SingleProjectState(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "02:30"
            )
        )
        advanceUntilIdle()

        Assert.assertEquals("-01:30", dateRepository.workTimeByDateChange.value)
    }

    @Test
    fun newDateWithoutWorkdayRow_usesGlobalDailyEstimateForworkTimeByDateEstimate() = runTest {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "08:00",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
            workdayStatsRows = emptyList()
        }
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository().apply {
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
            settings = SettingsState(
                dailyWorkTimeEstimate = "08:00",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            effectiveSettings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = ZERO_TIME
            )
        }
        val dateRepository = DateRepository().apply {
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
                SingleProjectState(
                    date = "2000-01-01",
                    projectName = "Alpha",
                    projectTime = "01:00"
                )
            )
        }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            settings = SettingsState(
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
                settingsRepository = settingsRepository,
                workdayRepository = workdayRepository
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

        override suspend fun getProject(date: String, projectName: String): SingleProjectState? = null

        override suspend fun getWorkTimeByDate(date: String): String =
            projectsByDateRange.filter { it.date == date }.fold(ZERO_TIME) { acc, p ->
                WorkTimeCalculator.calculateFlexTime(acc, p.projectTime)
            }
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        var settings: SettingsState? = null
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

        override suspend fun loadWorkday(date: String): String? = null

        override suspend fun upsertWorkdayStats(date: String, workTimeByDateEstimate: String) {
            val updatedRow = WorkdayStatsRow(
                date = date,
                workTimeByDateEstimate = workTimeByDateEstimate
            )
            workdayStatsRows = workdayStatsRows.filterNot { it.date == date } + updatedRow
            projectDetailsRepository?.let { repository ->
                repository.workdayStatsRows = workdayStatsRows
                repository.settings = (repository.settings ?: SettingsState()).copy(
                    dailyWorkTimeEstimate = workTimeByDateEstimate
                )
            }
        }

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> {
            return workdayStatsRows.filter { it.date in start..end }
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        var workTypes: List<String> = emptyList()
        var settings: SettingsState? = null
        var effectiveSettings: SettingsState? = null

        override suspend fun getSettings(): SettingsState? = settings

        override suspend fun insertSettings(settings: SettingsState) {
            this.settings = settings
        }

        override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? = effectiveSettings ?: settings

        override suspend fun getWorkTypes(): List<String> = workTypes

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun deleteAllWorkTypes() = Unit
    }
}

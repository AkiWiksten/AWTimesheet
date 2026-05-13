package com.akiwiksten.worktime30.feature.projects.details

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SettingsState
import com.akiwiksten.worktime30.domain.model.isNewDayForProject
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.SettingsRepository
import com.akiwiksten.worktime30.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadProjectDetails_withExplicitDateAndProjectName_loadsProjectDetailsData() = runTest {
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            projectDetails = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00",
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
        }
        val viewModel =
            ProjectDetailsViewModel(projectDetailsRepository, settingsRepository, DateRepository())

        viewModel.setProjectName("Alpha")
        viewModel.setDate("2026-04-10")
        viewModel.loadProjectDetails(date = "2026-04-10", projectName = "Alpha")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        Assert.assertTrue(state is ProjectDetailsUiState.Success)
        state as ProjectDetailsUiState.Success
        Assert.assertEquals("2026-04-10", state.details.date)
        Assert.assertEquals("Alpha", state.details.projectName)
        Assert.assertEquals("08:00", state.details.startTime)
        Assert.assertEquals("01:00", state.settings.initialFlexTimeTotal)
    }

    @Test
    fun loadProjectDetails_withProjectDetailsArg_mapsEntities_andClearDetailsResetsDailyFields() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "02:00"
            )
        }
        val viewModel = ProjectDetailsViewModel(
            FakeProjectDetailsRepository(),
            settingsRepository,
            DateRepository()
        )

        viewModel.loadProjectDetails(
            date = "2026-04-10",
            projectName = "Alpha",
            projectDetailsArg = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00",
            )
        )
        advanceUntilIdle()

        val persistedProjectDetails = viewModel.getProjectDetailsState()
        val settings = viewModel.getSettingsEstimatesState()
        Assert.assertEquals("Alpha", persistedProjectDetails.projectName)
        Assert.assertEquals("02:00", settings.initialFlexTimeTotal)

        viewModel.clearDetails()

        val cleared = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertEquals(ZERO_TIME, cleared.details.startTime)
        Assert.assertEquals(ZERO_TIME, cleared.details.endTime)
        Assert.assertEquals(ZERO_TIME, cleared.details.projectTime)
        Assert.assertEquals("00:30", cleared.details.lunchTimeEstimate)
        Assert.assertEquals("02:00", cleared.settings.initialFlexTimeTotal)
    }

    @Test
    fun setProjectTime_doesNotMutateInitialFlexTimeTotal() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "02:00"
            )
        }
        val viewModel = ProjectDetailsViewModel(
            FakeProjectDetailsRepository(),
            settingsRepository,
            DateRepository()
        )

        viewModel.loadProjectDetails(
            date = "2026-04-10",
            projectName = "Alpha",
            projectDetailsArg = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00",
            )
        )
        advanceUntilIdle()

        viewModel.setProjectTime("06:00")

        val updated = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertEquals("02:00", updated.settings.initialFlexTimeTotal)
    }

    @Test
    fun loadProjectDetails_newProjectOfDay_usesDateScopedLunchEstimate() = runTest {
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            projectDetails = null
        }
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:00",
                initialFlexTimeTotal = "01:00"
            )
            settingsByDate = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
        }
        val viewModel =
            ProjectDetailsViewModel(projectDetailsRepository, settingsRepository, DateRepository())

        viewModel.loadProjectDetails(date = "2026-04-10", projectName = "Alpha")
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertTrue(state.details.isNewDayForProject())
        Assert.assertEquals("00:30", state.details.lunchTimeEstimate)
    }

    @Test
    fun loadProjectDetails_withNewDayProjectDetailsArg_usesSettingsLunchTimeEstimate() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:45",
                initialFlexTimeTotal = "02:00"
            )
        }
        val viewModel = ProjectDetailsViewModel(
            FakeProjectDetailsRepository(),
            settingsRepository,
            DateRepository()
        )

        viewModel.loadProjectDetails(
            date = "2026-04-10",
            projectName = "Alpha",
            projectDetailsArg = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                lunchTimeEstimate = ZERO_TIME
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertTrue(state.details.isNewDayForProject())
        Assert.assertEquals("00:45", state.details.lunchTimeEstimate)
    }

    @Test
    fun loadProjectDetails_withExistingDayProjectDetailsArg_preservesProjectLunchTimeEstimate() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:45",
                initialFlexTimeTotal = "02:00"
            )
        }
        val viewModel = ProjectDetailsViewModel(
            FakeProjectDetailsRepository(),
            settingsRepository,
            DateRepository()
        )

        viewModel.loadProjectDetails(
            date = "2026-04-10",
            projectName = "Alpha",
            projectDetailsArg = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00",
                lunchTimeEstimate = "00:15"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertFalse(state.details.isNewDayForProject())
        Assert.assertEquals("00:15", state.details.lunchTimeEstimate)
    }

    @Test
    fun setProjectTime_whenOnlyProjectTime_usesSameDerivedFieldsAsOpenNormalization() = runTest {
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "02:00"
            )
        }
        val viewModel = ProjectDetailsViewModel(
            FakeProjectDetailsRepository(),
            settingsRepository,
            DateRepository()
        )

        viewModel.loadProjectDetails(
            date = "2026-04-10",
            projectName = "Alpha",
            projectDetailsArg = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha"
            )
        )
        advanceUntilIdle()

        viewModel.setProjectTime("02:00")

        val state = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertEquals("02:00", state.details.projectTime)
        Assert.assertEquals("02:30", state.details.endTime)
        Assert.assertEquals(ZERO_TIME, state.details.startTime)
        Assert.assertEquals(ZERO_TIME, state.details.lunchStart)
        Assert.assertEquals(ZERO_TIME, state.details.lunchEnd)
        Assert.assertEquals(ZERO_TIME, state.details.breakStart)
        Assert.assertEquals(ZERO_TIME, state.details.breakEnd)
    }

    @Test
    fun observeDateRepository_whenReObserved_doesNotLetOlderLoadOverwriteNewerDetails() = runTest {
        val dateRepository = DateRepository().apply { updateDate("2026-04-10") }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            delayMsByProjectName["Alpha"] = 1_000L
        }
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
        }
        val viewModel = ProjectDetailsViewModel(projectDetailsRepository, settingsRepository, dateRepository)

        viewModel.observeDateRepository(
            ProjectDetailsState(date = "2026-04-10", projectName = "Alpha", projectTime = "01:00")
        )
        viewModel.observeDateRepository(
            ProjectDetailsState(date = "2026-04-10", projectName = "Beta", projectTime = "02:00")
        )
        dateRepository.updateDate("2026-04-11")
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertEquals("2026-04-11", state.details.date)
        Assert.assertEquals("Beta", state.details.projectName)
        Assert.assertEquals("02:00", state.details.projectTime)
    }

    @Test
    fun loadProjectDetails_withDetailedProjectDetailsArg_prefersArgOverPersistedDetails() = runTest {
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            projectDetails = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00"
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
        }
        val viewModel = ProjectDetailsViewModel(projectDetailsRepository, settingsRepository, DateRepository())

        viewModel.loadProjectDetails(
            date = "2026-04-10",
            projectName = "Alpha",
            projectDetailsArg = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "09:00",
                endTime = "17:00",
                projectTime = "07:00"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertEquals("09:00", state.details.startTime)
        Assert.assertEquals("17:00", state.details.endTime)
        Assert.assertEquals("07:00", state.details.projectTime)
        Assert.assertEquals(0, projectDetailsRepository.getProjectDetailsCallCount)
    }

    @Test
    fun loadProjectDetails_withProjectTimeOnlyArg_prefersPersistedDetailsWhenAvailable() = runTest {
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            projectDetails = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00"
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
        }
        val viewModel = ProjectDetailsViewModel(projectDetailsRepository, settingsRepository, DateRepository())

        viewModel.loadProjectDetails(
            date = "2026-04-10",
            projectName = "Alpha",
            projectDetailsArg = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "02:00"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertEquals("08:00", state.details.startTime)
        Assert.assertEquals("16:00", state.details.endTime)
        Assert.assertEquals("08:00", state.details.projectTime)
    }

    @Test
    fun observeDateRepository_afterProjectNameChange_usesLatestProjectNameOnDateUpdates() = runTest {
        val dateRepository = DateRepository().apply { updateDate("2026-04-10") }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            projectDetails = ProjectDetailsState(
                date = "2026-04-11",
                projectName = "Beta",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00"
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
        }
        val viewModel = ProjectDetailsViewModel(projectDetailsRepository, settingsRepository, dateRepository)

        viewModel.observeDateRepository(ProjectDetailsState(date = "2026-04-10", projectName = "Alpha"))
        advanceUntilIdle()
        viewModel.setProjectName("Beta")
        val callsBeforeDateChange = projectDetailsRepository.getProjectDetailsCallCount

        dateRepository.updateDate("2026-04-11")
        advanceUntilIdle()

        Assert.assertEquals(callsBeforeDateChange, projectDetailsRepository.getProjectDetailsCallCount)
        val state = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertEquals("Beta", state.details.projectName)
        Assert.assertEquals("2026-04-11", state.details.date)
    }

    @Test
    fun loadProjectDetails_blankDate_shortCircuitsWithoutRepositoryCall() = runTest {
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            projectDetails = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00"
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
        }
        val viewModel = ProjectDetailsViewModel(projectDetailsRepository, settingsRepository, DateRepository())

        viewModel.loadProjectDetails(date = "2026-04-10", projectName = "Alpha")
        advanceUntilIdle()
        val callsAfterValidLoad = projectDetailsRepository.getProjectDetailsCallCount
        Assert.assertTrue(viewModel.isInitialLoadComplete.value)

        viewModel.loadProjectDetails(date = "", projectName = "Alpha")
        advanceUntilIdle()

        Assert.assertEquals(callsAfterValidLoad, projectDetailsRepository.getProjectDetailsCallCount)
        Assert.assertTrue(viewModel.isInitialLoadComplete.value)
    }

    @Test
    fun observeDateRepository_withRapidDateUpdates_keepsLatestDateState() = runTest {
        val dateRepository = DateRepository().apply { updateDate("2026-04-10") }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            projectDetails = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00"
            )
            delayMsByDate["2026-04-11"] = 1_000L
            delayMsByDate["2026-04-12"] = 0L
        }
        val settingsRepository = FakeSettingsRepository().apply {
            settings = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
        }
        val viewModel = ProjectDetailsViewModel(projectDetailsRepository, settingsRepository, dateRepository)

        viewModel.observeDateRepository(ProjectDetailsState(date = "2026-04-10", projectName = "Alpha"))
        advanceUntilIdle()

        dateRepository.updateDate("2026-04-11")
        dateRepository.updateDate("2026-04-12")
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertEquals("2026-04-12", state.details.date)
        Assert.assertEquals("Alpha", state.details.projectName)
    }

    @Test
    fun getProjectDetailsState_beforeSuccessfulLoad_throwsClearMessage() = runTest {
        val viewModel = ProjectDetailsViewModel(
            FakeProjectDetailsRepository(),
            FakeSettingsRepository(),
            DateRepository()
        )

        val error = Assert.assertThrows(IllegalStateException::class.java) {
            viewModel.getProjectDetailsState()
        }

        Assert.assertEquals(
            "Project details are unavailable before successful load.",
            error.message
        )
    }

    @Test
    fun getSettingsEstimatesState_beforeSuccessfulLoad_throwsClearMessage() = runTest {
        val viewModel = ProjectDetailsViewModel(
            FakeProjectDetailsRepository(),
            FakeSettingsRepository(),
            DateRepository()
        )

        val error = Assert.assertThrows(IllegalStateException::class.java) {
            viewModel.getSettingsEstimatesState()
        }

        Assert.assertEquals(
            "Settings are unavailable before successful load.",
            error.message
        )
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        var projectDetails: ProjectDetailsState? = null
        var projectDetailsByDateRangeResult: List<ProjectDetailsState> = emptyList()
        val delayMsByProjectName = mutableMapOf<String, Long>()
        val delayMsByDate = mutableMapOf<String, Long>()
        var getProjectDetailsCallCount: Int = 0
        var lastRequestedDate: String? = null
        var lastRequestedProjectName: String? = null

        override suspend fun getProjectDetails(
            date: String,
            projectName: String
        ): ProjectDetailsState? {
            getProjectDetailsCallCount++
            lastRequestedDate = date
            lastRequestedProjectName = projectName
            delay(timeMillis = delayMsByDate[date] ?: 0L)
            delay(timeMillis = delayMsByProjectName[projectName] ?: 0L)
            return projectDetails
        }

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) {
            this.projectDetails = projectDetails
        }

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) {
            this.projectDetails = null
        }

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsState> = projectDetailsByDateRangeResult
    }

    private class FakeSettingsRepository : SettingsRepository {
        var settings: SettingsState? = null
        var settingsByDate: SettingsState? = null

        override suspend fun getSettings(): SettingsState? = settings

        override suspend fun insertSettings(settings: SettingsState) = Unit

        override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? = settingsByDate ?: settings

        override suspend fun getWorkTypes(): List<String> = emptyList()

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun deleteAllWorkTypes() = Unit
    }
}

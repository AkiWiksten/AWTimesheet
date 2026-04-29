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
    fun setDate_andProjectName_loadsProjectDetailsData() = runTest {
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
        advanceUntilIdle()

        val state = viewModel.uiState.value
        Assert.assertTrue(state is ProjectDetailsUiState.Success)
        state as ProjectDetailsUiState.Success
        Assert.assertEquals("2026-04-10", state.data.date)
        Assert.assertEquals("Alpha", state.data.projectName)
        Assert.assertEquals("08:00", state.data.startTime)
        Assert.assertEquals("01:00", state.settings.initialFlexTimeTotal)
    }

    @Test
    fun loadProjectDetails_withArgs_mapsEntities_andClearDayResetsDailyFields() = runTest {
        val viewModel = ProjectDetailsViewModel(
            FakeProjectDetailsRepository(),
            FakeSettingsRepository(),
            DateRepository()
        )

        viewModel.setDate("2026-04-10")
        viewModel.setProjectName("Alpha")
        viewModel.loadProjectDetails(
            projectDetailsArg = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00",
            ),
            settingsArg = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "02:00"
            )
        )
        advanceUntilIdle()

        val persistedProjectDetails = viewModel.getProjectDetailsState()
        val settings = viewModel.getSettingsEstimatesState()
        Assert.assertEquals("Alpha", persistedProjectDetails.projectName)
        Assert.assertEquals("02:00", settings.initialFlexTimeTotal)

        viewModel.clearDay()

        val cleared = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertEquals(ZERO_TIME, cleared.data.startTime)
        Assert.assertEquals(ZERO_TIME, cleared.data.endTime)
        Assert.assertEquals(ZERO_TIME, cleared.data.projectTime)
        Assert.assertEquals("02:00", cleared.settings.initialFlexTimeTotal)
    }

    @Test
    fun setProjectTime_doesNotMutateInitialFlexTimeTotal() = runTest {
        val viewModel = ProjectDetailsViewModel(
            FakeProjectDetailsRepository(),
            FakeSettingsRepository(),
            DateRepository()
        )

        viewModel.setDate("2026-04-10")
        viewModel.setProjectName("Alpha")
        viewModel.loadProjectDetails(
            projectDetailsArg = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00",
            ),
            settingsArg = SettingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "02:00"
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

        viewModel.setDate("2026-04-10")
        viewModel.setProjectName("Alpha")
        viewModel.loadProjectDetails()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertTrue(state.data.isNewDayForProject())
        Assert.assertEquals("00:30", state.data.lunchTimeEstimate)
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        var projectDetails: ProjectDetailsState? = null
        var projectDetailsByDateRangeResult: List<ProjectDetailsState> = emptyList()

        override suspend fun getProjectDetails(
            date: String,
            projectName: String
        ): ProjectDetailsState? = projectDetails

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

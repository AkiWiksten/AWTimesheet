package com.akiwiksten.worktime30.feature.projects.details

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.WorkStatsState
import com.akiwiksten.worktime30.domain.model.isNewDayForProject
import com.akiwiksten.worktime30.domain.repository.DateRepository
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.WorkStatsRepository
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
        val workStatsRepository = FakeWorkStatsRepository().apply {
            workStats = WorkStatsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
        }
        val viewModel =
            ProjectDetailsViewModel(projectDetailsRepository, workStatsRepository, DateRepository())

        viewModel.setProjectName("Alpha")
        viewModel.setDate("2026-04-10")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        Assert.assertTrue(state is ProjectDetailsUiState.Success)
        state as ProjectDetailsUiState.Success
        Assert.assertEquals("2026-04-10", state.data.date)
        Assert.assertEquals("Alpha", state.data.projectName)
        Assert.assertEquals("08:00", state.data.startTime)
        Assert.assertEquals("01:00", state.data.workStats.initialFlexTimeTotal)
    }

    @Test
    fun loadProjectDetails_withArgs_mapsEntities_andClearDayResetsDailyFields() = runTest {
        val viewModel = ProjectDetailsViewModel(
            FakeProjectDetailsRepository(),
            FakeWorkStatsRepository(),
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
            workStatsArg = WorkStatsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "02:00"
            )
        )
        advanceUntilIdle()

        val persistedProjectDetails = viewModel.getProjectDetailsState()
        val workStats = viewModel.getWorkStatsState()
        Assert.assertEquals("Alpha", persistedProjectDetails.projectName)
        Assert.assertEquals("02:00", workStats.initialFlexTimeTotal)

        viewModel.clearDay()

        val cleared = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertEquals(ZERO_TIME, cleared.data.startTime)
        Assert.assertEquals(ZERO_TIME, cleared.data.endTime)
        Assert.assertEquals(ZERO_TIME, cleared.data.projectTime)
        Assert.assertEquals("02:00", cleared.data.workStats.initialFlexTimeTotal)
    }

    @Test
    fun setProjectTime_doesNotMutateInitialFlexTimeTotal() = runTest {
        val viewModel = ProjectDetailsViewModel(
            FakeProjectDetailsRepository(),
            FakeWorkStatsRepository(),
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
            workStatsArg = WorkStatsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "02:00"
            )
        )
        advanceUntilIdle()

        viewModel.setProjectTime("06:00")

        val updated = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertEquals("02:00", updated.data.workStats.initialFlexTimeTotal)
    }

    @Test
    fun loadProjectDetails_newProjectOfDay_usesDateScopedLunchEstimate() = runTest {
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            projectDetails = null
        }
        val workStatsRepository = FakeWorkStatsRepository().apply {
            workStats = WorkStatsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:00",
                initialFlexTimeTotal = "01:00"
            )
            workStatsByDate = WorkStatsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
        }
        val viewModel =
            ProjectDetailsViewModel(projectDetailsRepository, workStatsRepository, DateRepository())

        viewModel.setDate("2026-04-10")
        viewModel.setProjectName("Alpha")
        viewModel.loadProjectDetails()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertTrue(state.data.isNewDayForProject())
        Assert.assertEquals("00:30", state.data.workStats.dailyLunchTimeEstimate)
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

    private class FakeWorkStatsRepository : WorkStatsRepository {
        var workStats: WorkStatsState? = null
        var workStatsByDate: WorkStatsState? = null

        override suspend fun getWorkStats(): WorkStatsState? = workStats

        override suspend fun insertWorkStats(workStats: WorkStatsState) {
            this.workStats = workStats
        }

        override suspend fun getWorkStatsByDate(date: String): WorkStatsState? = workStatsByDate ?: workStats
    }
}
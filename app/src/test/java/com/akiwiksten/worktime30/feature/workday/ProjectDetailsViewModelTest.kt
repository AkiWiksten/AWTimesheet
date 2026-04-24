package com.akiwiksten.worktime30.feature.workday

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsUiState
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsViewModel
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import com.akiwiksten.worktime30.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun setDate_andProjectName_loadsProjectDetailsData() = runTest {
        val repository = FakeProjectDetailsRepository().apply {
            projectDetails = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00",
                flexTimeToday = "00:30"
            )
            workStats = WorkStatsState(
                dailyWorkTime = "07:30",
                lunchTime = "00:30",
                flexTimeTotal = "01:00"
            )
        }
        val viewModel = ProjectDetailsViewModel(repository, DateRepository())

        viewModel.setProjectName("Alpha")
        viewModel.setDate("2026-04-10")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProjectDetailsUiState.Success)
        state as ProjectDetailsUiState.Success
        assertEquals("2026-04-10", state.data.date)
        assertEquals("Alpha", state.data.projectName)
        assertEquals("08:00", state.data.startTime)
        assertEquals("01:00", state.data.workStats.flexTimeTotal)
    }

    @Test
    fun loadProjectDetails_withArgs_mapsEntities_andClearDayResetsDailyFields() = runTest {
        val viewModel = ProjectDetailsViewModel(FakeProjectDetailsRepository(), DateRepository())

        viewModel.setDate("2026-04-10")
        viewModel.setProjectName("Alpha")
        viewModel.loadProjectDetails(
            projectDetailsArg = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00",
                flexTimeToday = "00:30"
            ),
            workStatsArg = WorkStatsState(
                dailyWorkTime = "07:30",
                lunchTime = "00:30",
                flexTimeTotal = "02:00"
            )
        )
        advanceUntilIdle()

        val persistedProjectDetails = viewModel.getProjectDetailsState()
        val workStats = viewModel.getWorkStatsState()
        assertEquals("Alpha", persistedProjectDetails.projectName)
        assertEquals("02:00", workStats.flexTimeTotal)

        viewModel.clearDay()

        val cleared = viewModel.uiState.value as ProjectDetailsUiState.Success
        assertEquals(ZERO_TIME, cleared.data.startTime)
        assertEquals(ZERO_TIME, cleared.data.endTime)
        assertEquals(ZERO_TIME, cleared.data.projectTime)
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        var projectDetails: ProjectDetailsState? = null
        var workStats: WorkStatsState? = null

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

        override suspend fun getWorkStats(): WorkStatsState? = workStats

        override suspend fun insertWorkStats(workStats: WorkStatsState) {
            this.workStats = workStats
        }

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsState> = emptyList()
    }
}

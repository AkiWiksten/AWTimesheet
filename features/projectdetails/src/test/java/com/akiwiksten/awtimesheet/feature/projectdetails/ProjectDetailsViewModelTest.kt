package com.akiwiksten.awtimesheet.feature.projectdetails

import com.akiwiksten.awtimesheet.test.FakeProjectDetailsRepository
import com.akiwiksten.awtimesheet.test.FakeSettingsRepository
import com.akiwiksten.awtimesheet.test.InMemoryDateRepository
import com.akiwiksten.awtimesheet.test.MainDispatcherRule
import com.akiwiksten.awtimesheet.test.projectDetailsState
import com.akiwiksten.awtimesheet.test.settingsState
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
    fun loadProjectDetails_withExplicitDateAndProjectName_loadsProjectDetailsData() = runTest {
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            projectDetails = projectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                startTime = "08:00",
                endTime = "16:00",
                projectTime = "08:00",
            )
        }
        val settingsRepository = FakeSettingsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
        }
        val viewModel = createViewModel(
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository
        )

        viewModel.loadProjectDetails(
            date = "2026-04-10",
            projectDetailsArg = projectDetailsState(date = "2026-04-10", projectName = "Alpha")
        )
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
    fun observeDateRepository_withRapidDateUpdates_keepsLatestDateState() = runTest {
        val dateRepository = InMemoryDateRepository().apply { updateDate("2026-04-10") }
        val projectDetailsRepository = FakeProjectDetailsRepository().apply {
            projectDetails = null
            delayMsByDate["2026-04-11"] = 1_000L
            delayMsByDate["2026-04-12"] = 0L
        }
        val settingsRepository = FakeSettingsRepository().apply {
            settings = settingsState(
                dailyWorkTimeEstimate = "07:30",
                dailyLunchTimeEstimate = "00:30",
                initialFlexTimeTotal = "01:00"
            )
        }
        val viewModel = createViewModel(
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )

        viewModel.observeDateRepository(detailsArgs = projectDetailsState(projectName = "Alpha"))
        advanceUntilIdle()

        dateRepository.updateDate("2026-04-11")
        dateRepository.updateDate("2026-04-12")
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProjectDetailsUiState.Success
        Assert.assertEquals("2026-04-12", state.details.date)
        Assert.assertEquals("Alpha", state.details.projectName)
    }

    private fun createViewModel(
        projectDetailsRepository: FakeProjectDetailsRepository,
        settingsRepository: FakeSettingsRepository,
        dateRepository: InMemoryDateRepository = InMemoryDateRepository()
    ): ProjectDetailsViewModel {
        return ProjectDetailsViewModel(
            projectDetailsRepository = projectDetailsRepository,
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
    }
}

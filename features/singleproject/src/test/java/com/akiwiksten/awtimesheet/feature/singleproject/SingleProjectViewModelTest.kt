package com.akiwiksten.awtimesheet.feature.singleproject

import com.akiwiksten.awtimesheet.domain.repository.DateRepository
import com.akiwiksten.awtimesheet.domain.usecase.DeleteDraftProjectUseCase
import com.akiwiksten.awtimesheet.domain.usecase.SaveWorkdayUseCase
import com.akiwiksten.awtimesheet.test.FakeProjectDetailsRepository
import com.akiwiksten.awtimesheet.test.FakeProjectRepository
import com.akiwiksten.awtimesheet.test.FakeSettingsRepository
import com.akiwiksten.awtimesheet.test.FakeWorkdayRepository
import com.akiwiksten.awtimesheet.test.InMemoryDateRepository
import com.akiwiksten.awtimesheet.test.MainDispatcherRule
import com.akiwiksten.awtimesheet.test.projectState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SingleProjectViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveProject_newProject_addsProjectTimeToTrackedChange() = runTest {
        val dateRepository = InMemoryDateRepository().apply { updateDate("2026-04-10") }
        val projectRepository = FakeProjectRepository()
        val viewModel = createViewModel(
            projectRepository = projectRepository,
            dateRepository = dateRepository
        )
        viewModel.setLocalizedFlexDayWorkType("Absence-Flex day")

        viewModel.initializeState(
            projectName = "",
            projectTime = "",
            isAddMode = true,
            listIndex = 0,
        )
        advanceUntilIdle()

        viewModel.saveProject(
            state = projectState(
                projectName = "Alpha",
                projectTime = "02:30"
            )
        )
        advanceUntilIdle()

        Assert.assertEquals("02:30", dateRepository.workTimeByDateChange.value)
    }

    @Test
    fun saveProject_existingProject_tracksOnlyProjectTimeDifference() = runTest {
        val dateRepository = InMemoryDateRepository().apply { updateDate("2026-04-10") }
        val projectRepository = FakeProjectRepository().apply {
            insertProject(
                projectState(
                    date = "2026-04-10",
                    projectName = "Alpha",
                    projectTime = "02:00"
                )
            )
        }
        val viewModel = createViewModel(
            projectRepository = projectRepository,
            dateRepository = dateRepository
        )

        viewModel.initializeState(
            projectName = "Alpha",
            projectTime = "",
            isAddMode = false,
            listIndex = 0,
        )
        advanceUntilIdle()

        viewModel.saveProject(
            state = projectState(
                projectName = "Alpha",
                projectTime = "05:30"
            )
        )
        advanceUntilIdle()

        Assert.assertEquals("03:30", dateRepository.workTimeByDateChange.value)
    }

    @Test
    fun saveProject_absenceFlexDay_replacesSameDayProjects_andTracksTotalDayDelta() = runTest {
        val dateRepository = InMemoryDateRepository().apply { updateDate("2026-04-10") }
        val projectRepository = FakeProjectRepository().apply {
            insertProject(
                projectState(
                    date = "2026-04-10",
                    projectName = "Alpha",
                    projectTime = "02:00"
                )
            )
            insertProject(
                projectState(
                    date = "2026-04-10",
                    projectName = "Beta",
                    projectTime = "01:00"
                )
            )
        }
        val viewModel = createViewModel(
            projectRepository = projectRepository,
            dateRepository = dateRepository
        )
        viewModel.setLocalizedFlexDayWorkType("Absence-Flex day")

        viewModel.initializeState(
            projectName = "",
            projectTime = "",
            isAddMode = true,
            listIndex = 0,
        )
        advanceUntilIdle()

        viewModel.saveProject(
            state = projectState(
                projectName = "Absence-Flex day",
                projectTime = "07:30",
                workType = "Absence-Flex day"
            )
        )
        advanceUntilIdle()

        Assert.assertEquals("04:30", dateRepository.workTimeByDateChange.value)
    }

    private fun createViewModel(
        projectRepository: FakeProjectRepository,
        dateRepository: DateRepository
    ): SingleProjectViewModel {
        val settingsRepository = FakeSettingsRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        return SingleProjectViewModel(
            projectRepository = projectRepository,
            saveWorkdayUseCase = SaveWorkdayUseCase(
                projectRepository = projectRepository,
                settingsRepository = settingsRepository,
                workdayRepository = FakeWorkdayRepository()
            ),
            deleteDraftProjectUseCase = DeleteDraftProjectUseCase(
                projectRepository = projectRepository,
                projectDetailsRepository = projectDetailsRepository
            ),
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
    }
}

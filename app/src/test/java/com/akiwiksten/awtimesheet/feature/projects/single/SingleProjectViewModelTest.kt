package com.akiwiksten.awtimesheet.feature.projects.single

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
import com.akiwiksten.awtimesheet.domain.usecase.SaveWorkdayUseCase
import com.akiwiksten.awtimesheet.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SingleProjectViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun saveProject_newProject_addsProjectTimeToTrackedChange() = runTest {
        val dateRepository = DateRepository().apply { updateDate("2026-04-10") }
        val projectRepository = FakeProjectRepository()
        val viewModel = createViewModel(
            projectRepository = projectRepository,
            dateRepository = dateRepository
        )

        viewModel.initializeState(SingleProjectState())
        advanceUntilIdle()

        viewModel.saveProject(
            state = SingleProjectState(
                projectName = "Alpha",
                projectTime = "02:30"
            )
        )
        advanceUntilIdle()

        assertEquals("02:30", dateRepository.workTimeByDateChange.value)
    }

    @Test
    fun saveProject_existingProject_tracksOnlyProjectTimeDifference() = runTest {
        val dateRepository = DateRepository().apply { updateDate("2026-04-10") }
        val projectRepository = FakeProjectRepository().apply {
            insertProject(
                SingleProjectState(
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
            SingleProjectState(
                projectName = "Alpha",
                date = "2026-04-10"
            )
        )
        advanceUntilIdle()

        viewModel.saveProject(
            state = SingleProjectState(
                projectName = "Alpha",
                projectTime = "05:30"
            )
        )
        advanceUntilIdle()

        assertEquals("03:30", dateRepository.workTimeByDateChange.value)
    }

    private fun createViewModel(
        projectRepository: FakeProjectRepository,
        dateRepository: DateRepository
    ): SingleProjectViewModel {
        val settingsRepository = FakeSettingsRepository()
        return SingleProjectViewModel(
            projectRepository = projectRepository,
            saveWorkdayUseCase = SaveWorkdayUseCase(
                projectRepository = projectRepository,
                projectDetailsRepository = FakeProjectDetailsRepository(),
                settingsRepository = settingsRepository,
                workdayRepository = FakeWorkdayRepository()
            ),
            settingsRepository = settingsRepository,
            dateRepository = dateRepository
        )
    }

    private class FakeProjectRepository : ProjectRepository {
        private val projects = linkedMapOf<String, SingleProjectState>()
        private val projectNames = linkedSetOf<String>()

        override suspend fun anyRecords(): Boolean = projects.isNotEmpty()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> {
            return projects.values.filter { it.date in start..end }
        }

        override suspend fun getProject(date: String, projectName: String): SingleProjectState? {
            return projects["$date|$projectName"]
        }

        override suspend fun insertProject(project: SingleProjectState) {
            projects["${project.date}|${project.projectName}"] = project
            projectNames += project.projectName
        }

        override suspend fun deleteProject(project: SingleProjectState) {
            projects.remove("${project.date}|${project.projectName}")
        }

        override suspend fun getProjectNames(): List<String> = projectNames.toList()

        override suspend fun insertProjectName(projectName: String) {
            projectNames += projectName
        }

        override suspend fun deleteProjectName(projectName: String) {
            projectNames.remove(projectName)
        }

        override suspend fun isProjectNameUsed(projectName: String): Boolean {
            return projects.values.any { it.projectName == projectName }
        }

        override suspend fun getWorkTimeByDate(date: String): String {
            return projects.values
                .filter { it.date == date }
                .fold(ZERO_TIME) { total, project ->
                    WorkTimeCalculator.calculateFlexTime(total, project.projectTime)
                }
        }
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? = null

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun getProjectDetailsByDateRange(start: String, end: String): List<ProjectDetailsState> {
            return emptyList()
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        override suspend fun getSettings(): SettingsState? = null

        override suspend fun insertSettings(settings: SettingsState) = Unit

        override suspend fun getEffectiveSettingsForDate(date: String): SettingsState? = null

        override suspend fun getWorkTypes(): List<String> = emptyList()

        override suspend fun insertWorkType(workType: String) = Unit

        override suspend fun deleteWorkType(workType: String) = Unit

        override suspend fun deleteAllWorkTypes() = Unit
    }

    private class FakeWorkdayRepository : WorkdayRepository {
        override suspend fun loadWorkday(date: String): String? = null

        override suspend fun upsertWorkdayStats(date: String, workTimeByDateEstimate: String) = Unit

        override suspend fun getWorkdaysByDateRange(start: String, end: String): List<WorkdayStatsRow> = emptyList()
    }
}

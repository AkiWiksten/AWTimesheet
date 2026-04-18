package com.akiwiksten.worktime30.feature.projects

import com.akiwiksten.worktime30.data.database.entity.ProjectDetailsEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectEntity
import com.akiwiksten.worktime30.data.database.entity.ProjectNameEntity
import com.akiwiksten.worktime30.data.database.entity.SettingsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkStatsEntity
import com.akiwiksten.worktime30.data.database.entity.WorkTypeEntity
import com.akiwiksten.worktime30.data.repository.DateRepository
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.data.repository.SettingsRepository
import com.akiwiksten.worktime30.domain.DeleteProjectsUseCase
import com.akiwiksten.worktime30.domain.GetProjectsScreenDataUseCase
import com.akiwiksten.worktime30.domain.SaveProjectsUseCase
import com.akiwiksten.worktime30.feature.projects.daily.ProjectsUiState
import com.akiwiksten.worktime30.feature.projects.daily.ProjectsViewModel
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState
import com.akiwiksten.worktime30.test.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun selectedDate_loadsProjectsAsSuccessState() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectsByDateRange = listOf(ProjectEntity("2026-04-10", "Beta", "02:30"))
            projectNames = listOf(ProjectNameEntity("Beta"), ProjectNameEntity("Alpha"))
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val settingsRepository = FakeSettingsRepository().apply {
            workTypes = listOf(WorkTypeEntity("Office"))
        }
        val dateRepository = DateRepository()
        dateRepository.updateDate("2026-04-10")

        val viewModel = createViewModel(projectRepository, projectDetailsRepository, settingsRepository, dateRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is ProjectsUiState.Success)
        state as ProjectsUiState.Success
        assertEquals("2026-04-10", state.date)
        assertEquals("02:30", state.workTimeToday)
        assertEquals(listOf("Alpha", "Beta"), state.projects.map { it.projectName })
    }

    @Test
    fun saveProject_persistsProjectAndReloadsData() = runTest {
        val projectRepository = FakeProjectRepository().apply {
            projectNames = listOf(ProjectNameEntity("Alpha"))
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val settingsRepository = FakeSettingsRepository()
        val dateRepository = DateRepository()
        dateRepository.updateDate("2026-04-10")

        val viewModel = createViewModel(projectRepository, projectDetailsRepository, settingsRepository, dateRepository)
        advanceUntilIdle()

        viewModel.saveProject(
            SingleProjectState(
                projectName = "Alpha",
                projectTime = "01:00",
                allowance = "No Allowance"
            )
        )
        advanceUntilIdle()

        assertTrue(projectRepository.insertedProjects.any { it.projectName == "Alpha" })
        assertTrue(projectDetailsRepository.insertedProjectDetails.any { it.projectName == "Alpha" })
    }

    private fun createViewModel(
        projectRepository: FakeProjectRepository,
        projectDetailsRepository: FakeProjectDetailsRepository,
        settingsRepository: FakeSettingsRepository,
        dateRepository: DateRepository
    ): ProjectsViewModel {
        return ProjectsViewModel(
            getProjectsScreenDataUseCase = GetProjectsScreenDataUseCase(
                projectRepository = projectRepository,
                settingsRepository = settingsRepository
            ),
            saveProjectsUseCase = SaveProjectsUseCase(
                projectRepository = projectRepository,
                projectDetailsRepository = projectDetailsRepository
            ),
            deleteProjectsUseCase = DeleteProjectsUseCase(
                projectRepository = projectRepository,
                projectDetailsRepository = projectDetailsRepository
            ),
            projectDetailsRepository = projectDetailsRepository,
            dateRepository = dateRepository
        )
    }

    private class FakeProjectRepository : ProjectRepository {
        var projectsByDateRange: List<ProjectEntity> = emptyList()
        var projectNames: List<ProjectNameEntity> = emptyList()
        val insertedProjects = mutableListOf<ProjectEntity>()
        val deletedProjects = mutableListOf<ProjectEntity>()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<ProjectEntity> {
            return projectsByDateRange.filter { it.date in start..end }
        }

        override suspend fun insertProject(project: ProjectEntity) {
            insertedProjects += project
            projectsByDateRange = projectsByDateRange.filterNot {
                it.date == project.date && it.projectName == project.projectName
            } + project
        }

        override suspend fun deleteProject(project: ProjectEntity) {
            deletedProjects += project
            projectsByDateRange = projectsByDateRange.filterNot {
                it.date == project.date && it.projectName == project.projectName
            }
        }

        override suspend fun getProjectNames(): List<ProjectNameEntity> = projectNames

        override suspend fun insertProjectName(projectName: ProjectNameEntity) {
            if (this.projectNames.none { it.name == projectName.name }) {
                this.projectNames = this.projectNames + projectName
            }
        }

        override suspend fun deleteProjectName(projectName: ProjectNameEntity) {
            this.projectNames = this.projectNames.filterNot { it.name == projectName.name }
        }

        override suspend fun isProjectNameUsed(projectName: String): Boolean {
            return projectsByDateRange.any { it.projectName == projectName }
        }
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        var workStats: WorkStatsEntity? = null
        val insertedProjectDetails = mutableListOf<ProjectDetailsEntity>()

        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsEntity? {
            return insertedProjectDetails.firstOrNull { it.date == date && it.projectName == projectName }
        }

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsEntity) {
            insertedProjectDetails += projectDetails
        }

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsEntity) {
            insertedProjectDetails
                .removeIf {
                    it.date == projectDetails.date && it.projectName == projectDetails.projectName
                }
        }

        override suspend fun getWorkStats(): WorkStatsEntity? = workStats

        override suspend fun insertWorkStats(workStats: WorkStatsEntity) {
            this.workStats = workStats
        }

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsEntity> = emptyList()
    }

    private class FakeSettingsRepository : SettingsRepository {
        var workTypes: List<WorkTypeEntity> = emptyList()

        override suspend fun getSettings(): SettingsEntity? = null

        override suspend fun insertSettings(settings: SettingsEntity) = Unit

        override suspend fun getWorkTypes(): List<WorkTypeEntity> = workTypes

        override suspend fun insertWorkType(workType: WorkTypeEntity) = Unit

        override suspend fun deleteWorkType(workType: WorkTypeEntity) = Unit

        override suspend fun clearWorkTypes() = Unit
    }
}

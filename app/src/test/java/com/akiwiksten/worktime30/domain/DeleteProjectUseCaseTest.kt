package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.core.calculator.WorkTimeCalculator
import com.akiwiksten.worktime30.domain.model.ProjectDetailsState
import com.akiwiksten.worktime30.domain.model.SingleProjectState
import com.akiwiksten.worktime30.domain.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.domain.repository.ProjectRepository
import com.akiwiksten.worktime30.domain.usecase.DeleteProjectUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteProjectUseCaseTest {

    @Test
    fun invoke_nonZeroTime_deletesProjectAndProjectDetails_andDeletesUnusedProjectName() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            isProjectNameUsedByName["Beta"] = false
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()

        val useCase = DeleteProjectUseCase(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository
        )

        useCase(date = "2026-04-10", projectName = "Beta", projectTime = "01:00")

        assertEquals(
            listOf(SingleProjectState(date = "2026-04-10", projectName = "Beta", projectTime = "01:00")),
            projectRepository.deletedProjects
        )
        assertEquals(listOf("Beta"), projectRepository.deletedProjectNames)
        assertEquals(
            listOf(ProjectDetailsState(date = "2026-04-10", projectName = "Beta")),
            projectDetailsRepository.deletedProjectDetails
        )
    }

    @Test
    fun invoke_zeroTime_deletesOnlyProjectName_evenWhenStillUsed() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            isProjectNameUsedByName["Beta"] = true
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()

        val useCase = DeleteProjectUseCase(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository
        )

        useCase(date = "2026-04-10", projectName = "Beta")

        assertEquals(listOf("Beta"), projectRepository.deletedProjectNames)
        assertEquals(emptyList<SingleProjectState>(), projectRepository.deletedProjects)
        assertEquals(emptyList<ProjectDetailsState>(), projectDetailsRepository.deletedProjectDetails)
    }

    @Test
    fun invoke_nonZeroTime_doesNotDeleteProjectName_whenStillUsed() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            isProjectNameUsedByName["Beta"] = true
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()

        val useCase = DeleteProjectUseCase(
            projectRepository = projectRepository,
            projectDetailsRepository = projectDetailsRepository
        )

        useCase(date = "2026-04-10", projectName = "Beta", projectTime = "01:00")

        assertEquals(emptyList<String>(), projectRepository.deletedProjectNames)
    }

    private class FakeProjectRepository : ProjectRepository {
        override suspend fun anyRecords(): Boolean = false

        val deletedProjects = mutableListOf<SingleProjectState>()
        val deletedProjectNames = mutableListOf<String>()
        val isProjectNameUsedByName = mutableMapOf<String, Boolean>()
        val projectsByDateRange = mutableMapOf<String, List<SingleProjectState>>()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> {
            return projectsByDateRange[start] ?: emptyList()
        }

        override suspend fun insertProject(project: SingleProjectState) = Unit

        override suspend fun deleteProject(project: SingleProjectState) {
            deletedProjects += project
        }

        override suspend fun getProjectNames(): List<String> = emptyList()

        override suspend fun insertProjectName(projectName: String) = Unit

        override suspend fun deleteProjectName(projectName: String) {
            deletedProjectNames += projectName
        }

        override suspend fun isProjectNameUsed(projectName: String): Boolean =
            isProjectNameUsedByName[projectName] ?: false

        override suspend fun getWorkTimeByDate(date: String): String =
            (projectsByDateRange[date] ?: emptyList()).fold(ZERO_TIME) { acc, p ->
                WorkTimeCalculator.calculateFlexTime(acc, p.projectTime)
            }
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        val deletedProjectDetails = mutableListOf<ProjectDetailsState>()

        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? = null

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) {
            deletedProjectDetails += projectDetails
        }

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsState> = emptyList()
    }
}

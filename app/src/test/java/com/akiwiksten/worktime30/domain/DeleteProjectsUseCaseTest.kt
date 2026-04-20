package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.core.ZERO_TIME
import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState
import com.akiwiksten.worktime30.feature.projects.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.details.WorkStatsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteProjectsUseCaseTest {

    @Test
    fun invoke_deletesProjectAndProjectDetails_andDeletesUnusedProjectName() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            isProjectNameUsedByName["Beta"] = false
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val useCase = DeleteProjectsUseCase(projectRepository, projectDetailsRepository)

        useCase(date = "2026-04-10", projectName = "Beta")

        assertEquals(
            listOf(SingleProjectState(date = "2026-04-10", projectName = "Beta", projectTime = ZERO_TIME)),
            projectRepository.deletedProjects
        )
        assertEquals(listOf("Beta"), projectRepository.deletedProjectNames)
        assertEquals(
            listOf(ProjectDetailsState(date = "2026-04-10", projectName = "Beta")),
            projectDetailsRepository.deletedProjectDetails
        )
    }

    @Test
    fun invoke_doesNotDeleteProjectName_whenStillUsed() = runBlocking {
        val projectRepository = FakeProjectRepository().apply {
            isProjectNameUsedByName["Beta"] = true
        }
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val useCase = DeleteProjectsUseCase(projectRepository, projectDetailsRepository)

        useCase(date = "2026-04-10", projectName = "Beta")

        assertEquals(emptyList<String>(), projectRepository.deletedProjectNames)
    }

    private class FakeProjectRepository : ProjectRepository {
        val deletedProjects = mutableListOf<SingleProjectState>()
        val deletedProjectNames = mutableListOf<String>()
        val isProjectNameUsedByName = mutableMapOf<String, Boolean>()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> = emptyList()

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
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        val deletedProjectDetails = mutableListOf<ProjectDetailsState>()

        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? = null

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) {
            deletedProjectDetails += projectDetails
        }

        override suspend fun getWorkStats(): WorkStatsState? = null

        override suspend fun insertWorkStats(workStats: WorkStatsState) = Unit

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsState> = emptyList()
    }
}

package com.akiwiksten.worktime30.domain

import com.akiwiksten.worktime30.data.repository.ProjectDetailsRepository
import com.akiwiksten.worktime30.data.repository.ProjectRepository
import com.akiwiksten.worktime30.feature.projects.daily.SingleProjectState
import com.akiwiksten.worktime30.feature.projects.single.details.ProjectDetailsState
import com.akiwiksten.worktime30.feature.projects.single.details.WorkStatsState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveProjectsUseCaseTest {

    @Test
    fun invoke_savesProjectsAndProjectDetails() = runBlocking {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val useCase = SaveProjectsUseCase(projectRepository, projectDetailsRepository)

        useCase(
            projectsToSave = listOf(
                SingleProjectState(
                    date = "2026-04-10",
                    projectName = "Alpha",
                    projectTime = "02:00"
                )
            ),
            projectDetailsToSave = ProjectDetailsState(
                date = "2026-04-10",
                projectName = "Alpha",
                projectTime = "07:00"
            )
        )

        assertEquals(listOf("Alpha"), projectRepository.insertedProjectNames)
        assertEquals(1, projectRepository.insertedProjects.size)
        assertEquals(1, projectDetailsRepository.insertedProjectDetails.size)
    }

    @Test
    fun invoke_doesNotInsertProjectDetails_whenProjectDetailsIsNull() = runBlocking {
        val projectRepository = FakeProjectRepository()
        val projectDetailsRepository = FakeProjectDetailsRepository()
        val useCase = SaveProjectsUseCase(projectRepository, projectDetailsRepository)

        useCase(
            projectsToSave = listOf(
                SingleProjectState(
                    date = "2026-04-10",
                    projectName = "Alpha",
                    projectTime = "01:00"
                )
            ),
            projectDetailsToSave = null
        )

        assertEquals(emptyList<ProjectDetailsState>(), projectDetailsRepository.insertedProjectDetails)
    }

    private class FakeProjectRepository : ProjectRepository {
        val insertedProjects = mutableListOf<SingleProjectState>()
        val insertedProjectNames = mutableListOf<String>()

        override suspend fun getProjectsByDateRange(start: String, end: String): List<SingleProjectState> = emptyList()

        override suspend fun insertProject(project: SingleProjectState) {
            insertedProjects += project
        }

        override suspend fun deleteProject(project: SingleProjectState) = Unit

        override suspend fun getProjectNames(): List<String> = emptyList()

        override suspend fun insertProjectName(projectName: String) {
            insertedProjectNames += projectName
        }

        override suspend fun deleteProjectName(projectName: String) = Unit

        override suspend fun isProjectNameUsed(projectName: String): Boolean =
            false
    }

    private class FakeProjectDetailsRepository : ProjectDetailsRepository {
        val insertedProjectDetails = mutableListOf<ProjectDetailsState>()

        override suspend fun getProjectDetails(date: String, projectName: String): ProjectDetailsState? = null

        override suspend fun insertProjectDetails(projectDetails: ProjectDetailsState) {
            insertedProjectDetails += projectDetails
        }

        override suspend fun deleteProjectDetails(projectDetails: ProjectDetailsState) = Unit

        override suspend fun getWorkStats(): WorkStatsState? = null

        override suspend fun insertWorkStats(workStats: WorkStatsState) = Unit

        override suspend fun getProjectDetailsByDateRange(
            start: String,
            end: String
        ): List<ProjectDetailsState> = emptyList()
    }
}
